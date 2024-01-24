package io.kluev.watchlist.app;

public record ProgressRequest(
        String seriesId,
        String videoId,
        Float progress
) {
}
