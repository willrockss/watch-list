package io.kluev.watchlist.infra.qbit;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class QBitClientImpl implements QBitClient {
    public static final String ID_TAG_TEMPLATE = "id=%s";

    private final RestClient restClient;
    private final RestClient restClientChecker;
    private final QBitClientProperties properties;

    @Override
    public EnqueuedTorr addTorrPaused(String torrFilePath, ContentItemIdentity contentItemIdentity) {
        val alreadyPresent = findByIdTagOrNull(contentItemIdentity);
        if (alreadyPresent != null) {
            log.info("Already present: {}", alreadyPresent);
            return alreadyPresent;
        }

        val body = new HttpHeaders();
        body.add("urls", torrFilePath);
        body.add("tags", String.format(ID_TAG_TEMPLATE, contentItemIdentity.value()));
        body.add("paused", Boolean.TRUE.toString());

        restClient.post()
                .uri(properties.getUrl() + "/api/v2/torrents/add")
                .body(body)
                .retrieve()
                .toBodilessEntity();

        val justAdded = findByIdTagOrNull(contentItemIdentity);
        Assert.notNull(justAdded, "Unable to find just added torr");
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

        log.info("Found {} torrs by tag {}", resp, idTagValue);
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
                .uri(properties.getUrl() + "/api/v2/torrents/resume")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean isAvailable() {
        try {
            restClientChecker.post()
                    .uri(properties.getUrl() + "/api/v2/app/webapiVersion")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("QBit is not available due to {}", e.toString());
            return false;
        }
    }

    private EnqueuedTorr toEnqueuedTorr(TorrDto torrDto) {
        return new EnqueuedTorr(torrDto.hash, torrDto.contentPath, torrDto.getCompletionOn());
    }

    private static class ResponseDto extends ArrayList<TorrDto> {}

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
