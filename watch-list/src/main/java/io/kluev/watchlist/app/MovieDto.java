package io.kluev.watchlist.app;

public record MovieDto(
        String id,
        String title,
        String status,
        @Deprecated // use localPath
        String path,
        String localPath,
        String contentStreamUrl
) {
}
