package io.kluev.watchlist.app;

import lombok.Builder;

@Builder
public record SeriesDto(
        String id,
        String title,
        @Deprecated // use localPath instead
        String toWatchEpisodePath,
        String localPath,
        String contentStreamUrl
) {
}
