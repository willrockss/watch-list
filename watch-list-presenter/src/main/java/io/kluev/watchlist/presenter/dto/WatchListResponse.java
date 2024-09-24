package io.kluev.watchlist.presenter.dto;

import java.util.List;
import java.util.Objects;

public record WatchListResponse(
        List<Series> series,
        List<Movie> movies
) {
    public WatchListResponse {
        series = Objects.requireNonNullElse(series, List.of());
        movies = Objects.requireNonNullElse(movies, List.of());
    }
}
