package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.config.props.VideoServerProperties;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class GetWatchListHandler {

    private final SeriesRepository seriesRepository;
    private final MovieRepository movieRepository;
    private final VideoServerProperties videoServerProperties;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @SneakyThrows
    public WatchListResponse handle() {
        return new WatchListResponse(
                getSeries().stream().map(this::map).toList(),
                getMovies().stream().map(this::map).toList()
        );
    }

    @SneakyThrows
    private List<Series> getSeries() {
        return executor.submit(seriesRepository::getInProgress)
                .get(2, TimeUnit.MINUTES);
    }

    @SneakyThrows
    private List<MovieItem> getMovies() {
        return executor.submit(movieRepository::getMoviesToWatch)
                .get(2, TimeUnit.MINUTES);
    }

    private SeriesDto map(Series series) {
        val path = series
                .getNextToWatchEpisode()
                .map(Episode::getFilename)
                .map(it -> series.getPath().resolve(it))
                .map(Path::toString)
                .orElse(null);

        return SeriesDto
                .builder()
                .id(series.getId().getValue())
                .title(series.getTitle())
                .toWatchEpisodePath(path)
                .localPath(path)
                .contentStreamUrl(getContentUrlOrNull(path))
                .build();
    }

    private MovieDto map(MovieItem movieItem) {
        return new MovieDto(
                movieItem.getExternalId(),
                movieItem.getFullTitle(),
                movieItem.getStatus(),
                movieItem.getFilePath(),
                movieItem.getFilePath(),
                getContentUrlOrNull(movieItem.getFilePath())
        );
    }

    private @Nullable String getContentUrlOrNull(@Nullable String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        return videoServerProperties.getBaseUrl()
                + videoServerProperties.getVideoPath()
                + URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}
