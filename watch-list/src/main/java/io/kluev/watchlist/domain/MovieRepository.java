package io.kluev.watchlist.domain;

import java.util.List;

public interface MovieRepository {
    void enlist(MovieItem movieItem);
    List<MovieItem> getMoviesToWatch();
}
