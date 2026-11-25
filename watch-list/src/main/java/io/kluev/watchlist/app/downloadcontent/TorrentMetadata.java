package io.kluev.watchlist.app.downloadcontent;

import org.springframework.util.Assert;

import java.util.List;

/**
 * Torrent metadata resolved from a magnet link / torrent URL via qBittorrent's
 * {@code torrents/fetchMetadata} endpoint, without adding the torrent to the download queue.
 */
public record TorrentMetadata(
        String name,
        long totalSize,
        List<TorrFile> files,
        String infoHashV1,
        String infoHashV2
) {
    public TorrentMetadata {
        Assert.notNull(name, "name cannot be null");
        Assert.isTrue(totalSize >= 0, "totalSize cannot be negative");
        Assert.notNull(files, "files cannot be null");
        files = List.copyOf(files);
    }

    public record TorrFile(String path, long size) {
        public TorrFile {
            Assert.notNull(path, "path cannot be null");
            Assert.isTrue(size >= 0, "size cannot be negative");
        }
    }
}
