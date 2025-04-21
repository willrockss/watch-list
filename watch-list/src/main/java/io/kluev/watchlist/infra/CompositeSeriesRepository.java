package io.kluev.watchlist.infra;

import io.kluev.watchlist.app.SeriesDto;
import io.kluev.watchlist.app.SeriesRepository;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.event.EpisodeWatched;
import io.kluev.watchlist.domain.event.SeasonWatched;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Optional;

/**
 * Temp repository during migration from NodeRed implementation to GoogleSheetAPI implementation
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeSeriesRepository implements SeriesRepository {

    private final NodeRedSeriesRepository nodeRedSeriesRepository;
    private final GoogleSheetsSeriesRepository googleSheetsSeriesRepository;

    @Override
    public List<SeriesDto> getAllInProgress() {
        return nodeRedSeriesRepository.getAllInProgress();
    }

    @Override
    public Optional<Series> getInProgressById(String seriesId) {
        return nodeRedSeriesRepository.getInProgressById(seriesId);
    }

    @Override
    public void save(Series series) {
        val hasEpisodeWatchedEvent = series.getEvents().stream().anyMatch(it -> it instanceof EpisodeWatched);
        if (hasEpisodeWatchedEvent) {
            nodeRedSeriesRepository.save(series);
        }
        val hasSeasonWatchedEvent = series.getEvents().stream().anyMatch(it -> it instanceof SeasonWatched);
        if (hasSeasonWatchedEvent) {
            googleSheetsSeriesRepository.save(series);
        }

        if (!hasEpisodeWatchedEvent && !hasSeasonWatchedEvent) {
            log.info("Season was not updated. Do nothing");
        }
    }
}
