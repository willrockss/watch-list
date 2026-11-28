package io.kluev.watchlist.app.downloadcontent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnqueuedTorrTest {

    private static final String TORR_HASH = "0123456789abcdef0123456789abcdef01234567";
    private static final String CONTENT_PATH = "/downloads/movie";

    @Test
    void rejectsNullInfoHash() {
        assertThatThrownBy(() -> new EnqueuedTorr(null, CONTENT_PATH, 0, 1024L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("infoHash");
    }

    @Test
    void rejectsNullContentPath() {
        assertThatThrownBy(() -> new EnqueuedTorr(TORR_HASH, null, 0, 1024L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentPath");
    }

    @Test
    void rejectsNegativeSize() {
        assertThatThrownBy(() -> new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void completionOnZeroOrNegativeMeansNotFinished() {
        assertThat(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L).isFinished()).isFalse();
        assertThat(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, -5, 1024L).isFinished()).isFalse();
        assertThat(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 50, 1024L).isFinished()).isTrue();
    }
}
