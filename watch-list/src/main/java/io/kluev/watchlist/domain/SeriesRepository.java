package io.kluev.watchlist.domain;

import java.util.List;
import java.util.Optional;

public interface SeriesRepository {
    List<Series> getInProgress();

    Optional<Series> getInProgressById(String seriesId);

    void save(Series series);
}
