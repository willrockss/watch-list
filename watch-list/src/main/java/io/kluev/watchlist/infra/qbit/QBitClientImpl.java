package io.kluev.watchlist.infra.qbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;
import io.kluev.watchlist.app.downloadcontent.EnqueuedTorr;
import io.kluev.watchlist.app.downloadcontent.QBitClient;
import io.kluev.watchlist.infra.config.props.QBitClientProperties;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final OkHttpClient okHttpClientChecker = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build();

    @Override
    public EnqueuedTorr addTorrPaused(String torrFilePath, ContentItemIdentity contentItemIdentity) {
        val alreadyPresent = findByIdTagOrNull(contentItemIdentity);
        if (alreadyPresent != null) {
            log.info("Already present: {}", alreadyPresent);
            return alreadyPresent;
        }

        RequestBody body = new MultipartBody.Builder()
                .addFormDataPart("tags", String.format(ID_TAG_TEMPLATE, contentItemIdentity.value()))
                .addFormDataPart("paused", Boolean.TRUE.toString())
                .addFormDataPart("torrents", torrFilePath, RequestBody.create(
                        new File(torrFilePath),
                        okhttp3.MediaType.parse("application/x-bittorrent"))
                )
                .setType(MultipartBody.FORM)
                .build();

        Request request = new Request.Builder()
                .url(properties.getUrl() + "/api/v2/torrents/add")
                .post(body)
                .build();

        try(Response response = okHttpClient.newCall(request).execute()) {
            val bodyOrNull = response.body() == null ? null : response.body().string();
            log.info("Response code: {} {} with body {}", response.code(), response.message(), bodyOrNull);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        val justAdded = findByIdTagOrNull(contentItemIdentity);
        Assert.notNull(justAdded, () -> "Unable to find just added torr by " + contentItemIdentity);
        return justAdded;
    }

    @Override
    @Nullable
    public EnqueuedTorr findByIdTagOrNull(ContentItemIdentity contentItemIdentity) {
        val idTagValue = ID_TAG_TEMPLATE.formatted(contentItemIdentity.value());
        val resp = restClient.get()
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

        val body = new LinkedMultiValueMap<String, String>();
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

        val body = new LinkedMultiValueMap<String, String>();
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
        val request = new Request.Builder()
                .url(properties.getUrl() + "/api/v2/app/webapiVersion")
                .build();
        try(val resp = okHttpClientChecker.newCall(request).execute()) {
            log.debug("Web API version: {}", resp.body() == null ? null : resp.body().string());
            return resp.isSuccessful();
        } catch (IOException e) {
            log.info("Unable to execute check availability request due to {}", e.toString());
            return false;
        }
    }

    private EnqueuedTorr toEnqueuedTorr(TorrDto torrDto) {
        return new EnqueuedTorr(torrDto.hash, torrDto.contentPath, torrDto.getCompletionOn());
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
    }
}
