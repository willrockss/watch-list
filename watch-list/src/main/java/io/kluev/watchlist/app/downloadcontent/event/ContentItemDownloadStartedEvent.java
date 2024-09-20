package io.kluev.watchlist.app.downloadcontent.event;

import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;

public record ContentItemDownloadStartedEvent(
        ContentItemIdentity identity,
        String contentPath
) {
}
