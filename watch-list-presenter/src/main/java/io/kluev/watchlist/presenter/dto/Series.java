package io.kluev.watchlist.presenter.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record Series(
        String id,
        String title,
        String toWatchEpisodePath,
        String contentStreamUrl,
        Integer audioTrack,
        Integer skipIntroOffsetSec
) {
}
