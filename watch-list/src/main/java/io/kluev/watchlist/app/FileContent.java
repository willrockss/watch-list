package io.kluev.watchlist.app;

import lombok.NonNull;

// TODO move to another app|module later
public record FileContent(
        @NonNull String filename,
        byte @NonNull [] bytes
) {
}
