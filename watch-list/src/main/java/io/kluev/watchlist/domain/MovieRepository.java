package io.kluev.watchlist.domain;

import java.time.LocalDate;
import java.util.List;

public interface MovieRepository {
    void enlist(MovieItem movieItem);
    List<MovieItem> getMoviesToWatch();
    void markWatched(String kinopoiskId, LocalDate watchedAt);
}
