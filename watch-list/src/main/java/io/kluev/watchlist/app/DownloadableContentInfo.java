package io.kluev.watchlist.app;

// TODO move to another app|module later
public record DownloadableContentInfo(
    String title,
    String link,
    Long size,
    Statistics statistics
) {

    public record Statistics(
            Integer downloadedTimes,
            Integer seeders,
            Integer peers
    ) {
    }
}

