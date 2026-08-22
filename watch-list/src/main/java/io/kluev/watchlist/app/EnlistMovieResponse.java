package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;

public record EnlistMovieResponse(
        MovieItem movieItem
) {
}
