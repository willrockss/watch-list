package io.kluev.watchlist.app;

// TODO move to another app|module later
public record DownloadedContent(
        String filename,
        byte[] bytes
) {
}
