package io.kluev.watchlist.app;

import java.util.List;

public record WatchListResponse(
        List<SeriesDto> series,
        List<MovieDto> movies
) {
}
