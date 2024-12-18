package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.Series;

import java.util.List;
import java.util.Optional;

public interface SeriesRepository {
    List<SeriesDto> getAllInProgress();

    Optional<Series> getInProgressById(String seriesId);

    void save(Series series);
}
