package io.kluev.watchlist.app;

public record ProgressRequestV2(
        VideoType videoType,
        String videoId,
        String videoPath,
        Float progress
) {
}
