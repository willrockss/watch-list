package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProgressHandlerV2 {

    // TODO Extract to separate facade-service
    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;

    private final LockService lockService;
    private final SeriesRepository seriesRepository;

    public ProgressResponse handle(ProgressRequestV2 request) {
        log.debug("Going to handle progress update {}", request);

        // TODO extract to specification
        if (request.progress() > 99.0) {
            val lock = lockService.acquireLock(request.videoId());
            if (lock == null) {
                log.debug("Unable to acquire to acquire lock to update progress for {}", request.videoId());
                return new ProgressResponse("Episode update already in progress");
            }
            try {
                var error = updateWatchedEpisode(request);
                if (error != null) {
                    return new ProgressResponse(error);
                }

                error = sendNotification(request);
                if (error != null) {
                    return new ProgressResponse(error);
                }
                log.info("Video {} should be marked as watched", request.videoId());
            } finally {
                lock.unlock();
            }
        }
        return new ProgressResponse();
    }

    private String updateWatchedEpisode(ProgressRequestV2 request) {
        val series = seriesRepository.getInProgressById(request.videoId()).orElse(null);
        if (series == null) {
            return "Cannot find series by %s to mark episode as watched".formatted(request.videoId());
        }

        val isSuccess = series.markEpisodeWatched(request.videoPath());
        if (!isSuccess) {
            return "Cannot find episode to mark as watched or it's already marked as watched";
        }
        seriesRepository.save(series);
        return null;
    }

    private String sendNotification(ProgressRequestV2 request) {
        try {
            val notificationTextParam = URLEncoder.encode(request.videoPath() + " should be marked as watched", StandardCharsets.UTF_8);
            val resp = restClient
                    .post()
                    .uri(properties.getUrl() + "/show-notification?text=" + notificationTextParam)
                    .retrieve()
                    .toEntity(String.class);
            if (resp.getStatusCode().isError()) {
                return resp.getBody();
            }
            return null;
        } catch (Exception e) {
            log.error("Unable to send notification", e);
            return "Unable to send notification due to " + e.getMessage();
        }
    }
}
