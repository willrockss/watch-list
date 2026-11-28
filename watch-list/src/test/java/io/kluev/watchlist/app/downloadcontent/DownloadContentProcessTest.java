package io.kluev.watchlist.app.downloadcontent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadContentProcessTest {

    private static final ContentItemIdentity IDENTITY = new ContentItemIdentity("kinopoisk-t100");
    private static final String TORR_FILE_PATH = "/tmp/kinopoisk-t100.torrent";
    private static final String MAGNET_LINK = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Movie";
    private static final String TORR_HASH = "0123456789abcdef0123456789abcdef01234567";
    private static final String CONTENT_PATH = "/downloads/movie";

    private final QBitClient qBitClient = mock(QBitClient.class);

    @Test
    void build_fails_whenNeitherFilePathNorLinkIsSet() {
        assertThatThrownBy(() -> DownloadContentProcess.builder()
                .contentItemIdentity(IDENTITY)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of torrFilePath or link");
    }

    @Test
    void build_fails_whenBothFilePathAndLinkAreSet() {
        assertThatThrownBy(() -> DownloadContentProcess.builder()
                .contentItemIdentity(IDENTITY)
                .torrFilePath(TORR_FILE_PATH)
                .link(MAGNET_LINK)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of torrFilePath or link");
    }

    @Test
    void build_acceptsFileOnlyAndLinkOnlyProcesses() {
        var fileProcess = fileProcess();
        assertThat(fileProcess.getTorrFilePath()).isEqualTo(TORR_FILE_PATH);
        assertThat(fileProcess.getLink()).isNull();

        var linkProcess = linkProcess();
        assertThat(linkProcess.getLink()).isEqualTo(MAGNET_LINK);
        assertThat(linkProcess.getTorrFilePath()).isNull();
    }

    @Test
    void enqueuePaused_fileProcess_routesToByFile() {
        var process = fileProcess();
        when(qBitClient.addTorrByFilePaused(eq(TORR_FILE_PATH), eq(IDENTITY)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));

        process.enqueuePaused(qBitClient);

        verify(qBitClient).addTorrByFilePaused(TORR_FILE_PATH, IDENTITY);
        verify(qBitClient, never()).addTorrByLinkPaused(anyString(), any());
        assertThat(process.getStatus()).isEqualTo(DownloadContentProcessStatus.PAUSED);
        assertThat(process.getTorrInfoHash()).isEqualTo(TORR_HASH);
        assertThat(process.getContentPath()).isEqualTo(CONTENT_PATH);
    }

    @Test
    void enqueuePaused_linkProcess_routesToByLink() {
        var process = linkProcess();
        when(qBitClient.addTorrByLinkPaused(eq(MAGNET_LINK), eq(IDENTITY)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));

        process.enqueuePaused(qBitClient);

        verify(qBitClient).addTorrByLinkPaused(MAGNET_LINK, IDENTITY);
        verify(qBitClient, never()).addTorrByFilePaused(anyString(), any());
        assertThat(process.getStatus()).isEqualTo(DownloadContentProcessStatus.PAUSED);
        assertThat(process.getTorrInfoHash()).isEqualTo(TORR_HASH);
        assertThat(process.getContentPath()).isEqualTo(CONTENT_PATH);
    }

    @Test
    void enqueuePaused_metadataFetchFailure_propagates_andProcessStaysInitial() {
        var process = linkProcess();
        when(qBitClient.addTorrByLinkPaused(eq(MAGNET_LINK), eq(IDENTITY)))
                .thenThrow(new RuntimeException("Unable to fetch metadata for " + MAGNET_LINK));

        assertThatThrownBy(() -> process.enqueuePaused(qBitClient))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to fetch metadata");
        assertThat(process.hasInitialStatus()).isTrue();
        assertThat(process.getTorrInfoHash()).isNull();
    }

    @Test
    void start_startsTorrentAndMarksProcessing() {
        var process = linkProcess();
        when(qBitClient.addTorrByLinkPaused(eq(MAGNET_LINK), eq(IDENTITY)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));
        process.enqueuePaused(qBitClient);
        when(qBitClient.findByIdTagOrNull(IDENTITY))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));

        process.start(qBitClient);

        verify(qBitClient, times(1)).findByIdTagOrNull(IDENTITY);
        verify(qBitClient).start(any(EnqueuedTorr.class));
        assertThat(process.getStatus()).isEqualTo(DownloadContentProcessStatus.PROCESSING);
        assertThat(process.getContentPath()).isEqualTo(CONTENT_PATH);
    }

    private DownloadContentProcess fileProcess() {
        return DownloadContentProcess.builder()
                .contentItemIdentity(IDENTITY)
                .torrFilePath(TORR_FILE_PATH)
                .build();
    }

    private DownloadContentProcess linkProcess() {
        return DownloadContentProcess.builder()
                .contentItemIdentity(IDENTITY)
                .link(MAGNET_LINK)
                .build();
    }
}
