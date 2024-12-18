package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.config.props.VideoServerProperties;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                getSeries(),
                getMovies().stream().map(this::map).toList()
        );
    }

    @SneakyThrows
    private List<SeriesDto> getSeries() {
        return executor.submit(seriesRepository::getAllInProgress)
                .get(2, TimeUnit.MINUTES);
    }

    @SneakyThrows
    private List<MovieItem> getMovies() {
        return executor.submit(movieRepository::getMoviesToWatch)
                .get(2, TimeUnit.MINUTES);
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
