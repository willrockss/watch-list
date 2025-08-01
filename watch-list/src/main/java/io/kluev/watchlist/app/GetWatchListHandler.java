package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.config.props.VideoServerProperties;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class GetWatchListHandler {

    private final SeriesRepository seriesRepository;
    private final MovieRepository movieRepository;
    private final VideoServerProperties videoServerProperties;
    private final ExecutorService executor;

    @SneakyThrows
    public WatchListResponse handle() {
        val seriesFuture = getSeriesFuture();
        val moviesFuture = getMoviesFuture();
        return new WatchListResponse(
                seriesFuture.get(2, TimeUnit.MINUTES),
                moviesFuture.get(2, TimeUnit.MINUTES)
                        .stream().map(this::map).toList()
        );
    }

    @SneakyThrows
    private Future<List<SeriesDto>> getSeriesFuture() {
        return executor.submit(seriesRepository::getAllInProgress);
    }

    @SneakyThrows
    private Future<List<MovieItem>> getMoviesFuture() {
        return executor.submit(movieRepository::getMoviesToWatch);

    }

    private MovieDto map(MovieItem movieItem) {
        return new MovieDto(
                movieItem.getExternalId(),
                movieItem.getFullTitle(),
                movieItem.getStatus(),
                movieItem.getFilePath(),
                movieItem.getFilePath(),
                getContentUrlOrNull(movieItem.getFilePath(), movieItem.getExternalId())
        );
    }

    private @Nullable String getContentUrlOrNull(@Nullable String path, @NonNull String id) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val requestPart = videoServerProperties.getVideoPathTemplate().formatted(
                id,
                UriUtils.encode(path, StandardCharsets.UTF_8),
                VideoType.MOVIE.name()
        );
        return videoServerProperties.getBaseUrl() + requestPart;
    }
}
