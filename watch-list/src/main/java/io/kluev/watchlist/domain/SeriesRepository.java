package io.kluev.watchlist.domain;

import java.util.List;

public interface SeriesRepository {
    List<Series> getInProgress();
}
