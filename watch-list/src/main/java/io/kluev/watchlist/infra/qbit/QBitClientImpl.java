package io.kluev.watchlist.infra.qbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;
import io.kluev.watchlist.app.downloadcontent.EnqueuedTorr;
import io.kluev.watchlist.app.downloadcontent.QBitClient;
import io.kluev.watchlist.app.downloadcontent.TorrentMetadata;
import io.kluev.watchlist.infra.config.props.QBitClientProperties;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * For some reason Spring's RestClient encodes Form-Data incorrectly (put content type on each form element) and
 * QBittorrent doesn't accept it. With OkHttpClient everything works fine out of the box.
 */
@Slf4j
@RequiredArgsConstructor
public class QBitClientImpl implements QBitClient {
    public static final String ID_TAG_TEMPLATE = "id=%s";

    private final RestClient restClient;
    private final QBitClientProperties properties;

    /**
     * All qBittorrent calls that participate in the magnet flow (fetchMetadata + add) must share
     * one WebUI session: fetched metadata is cached by qBittorrent per WebUI session and is
     * reused by {@code torrents/add} only within that same session. The cookie jar keeps the
     * {@code QBT_SID_*} cookie so every call lands in the same session.
     */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .cookieJar(new ConcurrentMapCookieJar())
            .build();
    private final OkHttpClient okHttpClientChecker = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build();

    private static final Duration METADATA_POLL_INTERVAL = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public EnqueuedTorr addTorrByFilePaused(String torrFilePath, ContentItemIdentity contentItemIdentity) {
        var alreadyPresent = findByIdTagOrNull(contentItemIdentity);
        if (alreadyPresent != null) {
            log.info("Already present: {}", alreadyPresent);
            return alreadyPresent;
        }

        RequestBody body = new MultipartBody.Builder()
                .addFormDataPart("tags", String.format(ID_TAG_TEMPLATE, contentItemIdentity.value()))
                .addFormDataPart("paused", Boolean.TRUE.toString()) // deprecated
                .addFormDataPart("stopped", Boolean.TRUE.toString())
                .addFormDataPart("autoTMM", Boolean.FALSE.toString())
                .addFormDataPart("torrents", torrFilePath, RequestBody.create(
                        new File(torrFilePath),
                        okhttp3.MediaType.parse("application/x-bittorrent"))
                )
                .setType(MultipartBody.FORM)
                .build();

        return addPaused(body, contentItemIdentity);
    }

    @Override
    public EnqueuedTorr addTorrByLinkPaused(String link, ContentItemIdentity contentItemIdentity) {
        Assert.isTrue(isSupportedTorrentLink(link),
                () -> "Unsupported torrent link (expected magnet: or http(s)://): " + link);
        var alreadyPresent = findByIdTagOrNull(contentItemIdentity);
        if (alreadyPresent != null) {
            log.info("Already present: {}", alreadyPresent);
            return alreadyPresent;
        }

        // Fetch metadata first: qBittorrent caches it in the session metadata cache and
        // torrents/add reuses it, so the torrent is added with metadata already in place
        // (a single metadata download, like adding a .torrent file). On failure/timeout —
        // abort with an exception and add nothing.
        var metadataTimeout = properties.getMetadataTimeout();
        var metadata = fetchMetadata(link, metadataTimeout);
        if (metadata == null) {
            throw new RuntimeException("Unable to fetch metadata for " + link + " within " + metadataTimeout
                    + " — torrent is NOT added");
        }
        log.info("Fetched metadata for {}: name='{}', totalSize={}, files={}",
                link, metadata.name(), metadata.totalSize(), metadata.files().size());

        RequestBody body = new MultipartBody.Builder()
                .addFormDataPart("tags", String.format(ID_TAG_TEMPLATE, contentItemIdentity.value()))
                .addFormDataPart("paused", Boolean.TRUE.toString()) // deprecated
                .addFormDataPart("stopped", Boolean.TRUE.toString())
                .addFormDataPart("autoTMM", Boolean.FALSE.toString())
                .addFormDataPart("urls", link)
                .setType(MultipartBody.FORM)
                .build();

        return addPaused(body, contentItemIdentity);
    }

    private EnqueuedTorr addPaused(RequestBody multipartBody, ContentItemIdentity contentItemIdentity) {
        Request request = new Request.Builder()
                .url(properties.getUrl() + "/api/v2/torrents/add")
                .post(multipartBody)
                .build();

        try(Response response = okHttpClient.newCall(request).execute()) {
            var bodyOrNull = response.body() == null ? null : response.body().string();
            log.info("Response code: {} {} with body {}", response.code(), response.message(), bodyOrNull);
            if (!response.isSuccessful() || "Fails.".equals(bodyOrNull)) {
                throw new RuntimeException("Unable to add torrent " + contentItemIdentity
                        + " to qBittorrent: response code " + response.code() + " with body " + bodyOrNull);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var justAdded = findByIdTagOrNull(contentItemIdentity);
        Assert.notNull(justAdded, () -> "Unable to find just added torr by " + contentItemIdentity);
        return justAdded;
    }

    private static boolean isSupportedTorrentLink(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        var normalized = link.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("magnet:")
                || normalized.startsWith("http://")
                || normalized.startsWith("https://");
    }

    @Nullable
    private TorrentMetadata fetchMetadataOnce(String link) {
        Assert.hasText(link, "link cannot be blank");

        // qBittorrent allows POST requests only for this endpoint (GET responds with 405).
        // Deliberately uses the same OkHttpClient (and thus the same WebUI session) as
        // torrents/add: the fetched metadata stays in the session's metadata cache and the
        // add reuses it instead of downloading it a second time.
        Request request = new Request.Builder()
                .url(properties.getUrl() + "/api/v2/torrents/fetchMetadata")
                .post(new FormBody.Builder()
                        .add("source", link)
                        .build())
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            final int code = response.code();
            final String bodyOrNull = response.body() == null ? null : response.body().string();
            log.info("fetchMetadata response for {}: code {} with body {}", link, code, bodyOrNull);
            if (code == 200) {
                return toTorrentMetadata(bodyOrNull, link);
            }
            if (code == 202) {
                // metadata is not resolved yet — the caller should retry later
                return null;
            }
            throw new RuntimeException("Unable to fetch torrent metadata for " + link
                    + ": response code " + code + " with body " + bodyOrNull);
        } catch (IOException e) {
            throw new RuntimeException("Unable to fetch torrent metadata for " + link, e);
        }
    }

    @Nullable
    private TorrentMetadata fetchMetadata(String link, Duration timeout) {
        Assert.notNull(timeout, "timeout cannot be null");
        Assert.isTrue(!timeout.isNegative() && !timeout.isZero(), "timeout must be positive");

        var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            var metadata = fetchMetadataOnce(link);
            if (metadata != null) {
                return metadata;
            }
            try {
                Thread.sleep(METADATA_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for torrent metadata of {}", link);
                return null;
            }
        }
        log.warn("Timed out after {} waiting for torrent metadata of {}", timeout, link);
        return null;
    }

    private TorrentMetadata toTorrentMetadata(String body, String link) {
        try {
            var dto = objectMapper.readValue(body, FetchMetadataResponseDto.class);
            Assert.notNull(dto.info, () -> "'info' section is missing in fetchMetadata response for " + link);
            var files = dto.info.files == null ? List.<TorrentMetadata.TorrFile>of() : dto.info.files.stream()
                    .map(fileDto -> new TorrentMetadata.TorrFile(fileDto.path, fileDto.length))
                    .toList();
            return new TorrentMetadata(dto.info.name, dto.info.length, files, dto.infoHashV1, dto.infoHashV2);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse fetchMetadata response for " + link + ": " + body, e);
        }
    }

    @Override
    @Nullable
    public EnqueuedTorr findByIdTagOrNull(ContentItemIdentity contentItemIdentity) {
        var idTagValue = ID_TAG_TEMPLATE.formatted(contentItemIdentity.value());
        var resp = restClient.get()
                .uri(properties.getUrl() + "/api/v2/torrents/info?tag={idTagValue}", idTagValue)
                .retrieve()
                .body(ResponseDto.class);

        log.debug("Found {} torrs by tag {}", resp, idTagValue);
        Assert.notNull(resp, "response should not be null!");

        if (resp.isEmpty()) {
            return null;
        } else {
            Assert.isTrue(resp.size() == 1, "response should contain only one item!");
            return toEnqueuedTorr(resp.getFirst());
        }
    }

    @Override
    public void deleteWithContent(@NonNull EnqueuedTorr torr) {
        Assert.notNull(torr, "torr cannot be null");

        var body = new LinkedMultiValueMap<String, String>();
        body.add("hashes", torr.infoHash());
        body.add("deleteFiles", Boolean.TRUE.toString());

        restClient.post()
                .uri(properties.getUrl() + "/api/v2/torrents/delete")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void start(@NonNull EnqueuedTorr torr) {
        Assert.notNull(torr, "torr cannot be null");

        var body = new LinkedMultiValueMap<String, String>();
        body.add("hashes", torr.infoHash());
        restClient.post()
                .uri(properties.getUrl() + "/api/v2/torrents/start")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean isAvailable() {
        var request = new Request.Builder()
                .url(properties.getUrl() + "/api/v2/app/webapiVersion")
                .build();
        try(var resp = okHttpClientChecker.newCall(request).execute()) {
            log.debug("Web API version: {}", resp.body() == null ? null : resp.body().string());
            return resp.isSuccessful();
        } catch (IOException e) {
            log.info("Unable to execute check availability request due to {}", e.toString());
            return false;
        }
    }

    @Override
    public DataSize getFreeSpaceOnDisk() {
        var resp = restClient.get()
                .uri(properties.getUrl() + "/api/v2/sync/maindata")
                .retrieve()
                .body(SyncMainDataDto.class);
        Assert.notNull(resp, "response should not be null!");
        var serverState = resp.serverState;
        Assert.notNull(serverState, "serverState should not be null!");
        return DataSize.ofBytes(serverState.freeSpaceOnDisk);
    }

    private EnqueuedTorr toEnqueuedTorr(TorrDto torrDto) {
        var contentPath = StringUtils.hasText(torrDto.contentPath) ? torrDto.contentPath : null;
        return new EnqueuedTorr(torrDto.hash, contentPath, torrDto.getCompletionOn(), torrDto.size);
    }

    private static class ResponseDto extends ArrayList<TorrDto> {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class TorrDto {
        @JsonProperty("hash")
        String hash;
        @JsonProperty("content_path")
        String contentPath;
        @JsonProperty("completion_on")
        int completionOn;
        @JsonProperty("size")
        long size;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class SyncMainDataDto {
        @JsonProperty("server_state")
        ServerStateDto serverState;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class ServerStateDto {
        @JsonProperty("free_space_on_disk")
        long freeSpaceOnDisk;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class FetchMetadataResponseDto {
        @JsonProperty("infohash_v1")
        String infoHashV1;
        @JsonProperty("infohash_v2")
        String infoHashV2;
        @JsonProperty("info")
        FetchMetadataInfoDto info;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class FetchMetadataInfoDto {
        @JsonProperty("name")
        String name;
        @JsonProperty("length")
        long length;
        @JsonProperty("files")
        List<FetchMetadataFileDto> files;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class FetchMetadataFileDto {
        @JsonProperty("path")
        String path;
        @JsonProperty("length")
        long length;
    }

}
