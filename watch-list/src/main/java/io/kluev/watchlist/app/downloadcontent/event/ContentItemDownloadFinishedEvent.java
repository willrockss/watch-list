package io.kluev.watchlist.app.downloadcontent.event;

import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;

public record ContentItemDownloadFinishedEvent(
        ContentItemIdentity identity
) {
}
