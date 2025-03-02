package io.kluev.watchlist.app.progress;

import io.kluev.watchlist.app.VideoType;

public record ProgressRequestV2(
        VideoType videoType,
        String videoId,
        String videoPath,
        Float progress
) {
}
