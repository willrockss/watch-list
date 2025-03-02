package io.kluev.watchlist.app.progress;

import io.kluev.watchlist.app.LockService;
import io.kluev.watchlist.app.SeriesRepository;
import io.kluev.watchlist.app.VideoType;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.WatchDateStrategy;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ProgressHandlerV2 {

    private final MovieRepository movieRepository;
    private final WatchDateStrategy watchDateStrategy;
    private final Clock clock;

    // TODO Extract to separate facade-service
    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;

    private final LockService lockService;
    private final SeriesRepository seriesRepository;

    private final Map<VideoType, VideoItemWatchedSpecification> watchedSpecs;

    public ProgressResponse handle(ProgressRequestV2 request) {
        log.debug("Going to handle progress update {}", request);

        val watchedSpec = getWatchedSpecification(request);
        if (watchedSpec.isWatched(request.progress())) {
            val lock = lockService.acquireLock(request.videoId());
            if (lock == null) {
                log.debug("Unable to acquire to acquire lock to update progress for {}", request.videoId());
                return new ProgressResponse("Episode update already in progress");
            }
            try {
                String error = null;
                switch (request.videoType()) {
                    case VideoType.EPISODE -> error = updateWatchedEpisode(request);
                    case VideoType.MOVIE -> error = updateWatchedMovie(request);
                }
                if (error != null) {
                    return new ProgressResponse(error);
                }

                error = sendNotification(request);
                if (error != null) {
                    return new ProgressResponse(error);
                }
                log.info("Video {} should be marked as watched. Used lock: {}", request.videoId(), lock);
            } finally {
                lock.unlock();
            }
        }
        return new ProgressResponse();
    }

    private VideoItemWatchedSpecification getWatchedSpecification(ProgressRequestV2 req) {
        val type = req.videoType();
        val byTypeSpec = watchedSpecs.get(type);
        if (byTypeSpec != null) {
            return byTypeSpec;
        }
        val defaultSpec = watchedSpecs.get(null);
        if (defaultSpec != null) {
            return defaultSpec;
        }
        throw new RuntimeException("Unable to find neither specific nor default specification for " + type);
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

    private String updateWatchedMovie(ProgressRequestV2 request) {
        try {
            movieRepository.markWatched(request.videoId(), watchDateStrategy.calculateWatchDate(clock));
            return null;
        } catch (Exception e) {
            return "Unable to mark movie %s as watched due to %s".formatted(request.videoId(), e);
        }
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
