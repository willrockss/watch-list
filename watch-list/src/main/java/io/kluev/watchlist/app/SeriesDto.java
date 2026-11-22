package io.kluev.watchlist.app;

import lombok.Builder;

@Builder
public record SeriesDto(
        String id,
        String title,
        String localPath,
        String contentStreamUrl,
        Integer audioTrack,
        Integer skipIntroOffsetSec
) {
}
