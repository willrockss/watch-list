package io.kluev.watchlist.presenter.dto;

public record PlayRequest(
        String videoId,
        String videoPath
) {
}
