package io.kluev.watchlist.app.downloadcontent;

import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.unit.DataSize;


public interface QBitClient {
    EnqueuedTorr addTorrByFilePaused(String torrFilePath, ContentItemIdentity id);

    /**
     * Adds a torrent paused by a link: a {@code magnet:?xt=...} URI or an http(s) URL to a .torrent file.
     */
    EnqueuedTorr addTorrByLinkPaused(String link, ContentItemIdentity id);

    @Nullable EnqueuedTorr findByIdTagOrNull(ContentItemIdentity id);
    void deleteWithContent(@NotNull EnqueuedTorr torr);
    void start(@NotNull EnqueuedTorr torr);
    boolean isAvailable();
    DataSize getFreeSpaceOnDisk();
}
