package io.kluev.watchlist.presenter.dto;

public record Series(
        String id,
        String title,
        String toWatchEpisodePath
) {
}
