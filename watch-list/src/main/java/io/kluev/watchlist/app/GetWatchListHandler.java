package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;

@RequiredArgsConstructor
public class GetWatchListHandler {

    private final SeriesRepository seriesRepository;

    public WatchListResponse handle() {
        val series = seriesRepository.getInProgress();
        return new WatchListResponse(
                series.stream().map(this::map).toList()
        );
    }

    private SeriesDto map(Series series) {
        val path = series
                .getNextToWatchEpisode()
                .map(Episode::getFilename)
                .map(it -> series.getPath().resolve(it))
                .map(Path::toString)
                .orElse(null);

        return new SeriesDto(
                series.getId().getValue(),
                series.getTitle(),
                path
        );
    }
}
