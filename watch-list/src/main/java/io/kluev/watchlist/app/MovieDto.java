package io.kluev.watchlist.app;

public record MovieDto(
        String id,
        String title,
        String status,
        String path
) {
}
