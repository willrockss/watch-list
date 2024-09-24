package io.kluev.watchlist.presenter.dto;

public record Movie(
        String id,
        String title,
        String status,
        String path
) {
}
