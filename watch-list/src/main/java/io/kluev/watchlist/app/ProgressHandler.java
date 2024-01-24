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
public class ProgressHandler {

    // TODO Extract to separate facade-service
    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;

    private final SeriesRepository seriesRepository;

    public ProgressResponse handle(ProgressRequest request) {
        log.debug("Going to update watch progress by {}", request);

        if (request.progress() > 99.0) {
            var error = updateWatchedEpisode(request);
            if (error != null) {
                return new ProgressResponse(error);
            }

            error = sendNotification(request);
            if (error != null) {
                return new ProgressResponse(error);
            }
        }
        return new ProgressResponse();
    }

    private String updateWatchedEpisode(ProgressRequest request) {
        val series = seriesRepository.getInProgressById(request.seriesId()).orElseThrow();
        val isSuccess = series.markEpisodeWatched(request.videoId());
        if (!isSuccess) {
            return "Cannot find episode to mark as watched";
        }

        seriesRepository.save(series);

        val text = URLEncoder.encode(request.videoId() + " should be marked as watched", StandardCharsets.UTF_8);
        val resp = restClient
                .post()
                .uri(properties.getUrl() + "/show-notification?text=" + text)
                .body(request)
                .retrieve()
                .toEntity(String.class);
        if (resp.getStatusCode().isError()) {
            return resp.getBody();
        }
        return null;
    }

    private String sendNotification(ProgressRequest request) {
        val notificationTextParam = URLEncoder.encode(request.videoId() + " should be marked as watched", StandardCharsets.UTF_8);
        val resp = restClient
                .post()
                .uri(properties.getUrl() + "/show-notification?text=" + notificationTextParam)
                .retrieve()
                .toEntity(String.class);
        if (resp.getStatusCode().isError()) {
            return resp.getBody();
        }
        return null;
    }
}
