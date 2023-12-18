package io.kluev.watchlist.app;

public record PlayRequest(
        String videoId,
        String videoPath
) {
}
