package io.kluev.watchlist.app.downloadcontent;

import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public interface QBitClient {
    EnqueuedTorr addTorrPaused(String torrPath, ContentItemIdentity id);
    @Nullable EnqueuedTorr findByIdTagOrNull(ContentItemIdentity id);
    void deleteWithContent(@NotNull EnqueuedTorr torr);
    void start(@NotNull EnqueuedTorr torr);
    boolean isAvailable();
}
