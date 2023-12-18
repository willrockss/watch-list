package io.kluev.watchlist.app;

public record SeriesDto(
        String id,
        String title,
        String toWatchEpisodePath
) {
}
