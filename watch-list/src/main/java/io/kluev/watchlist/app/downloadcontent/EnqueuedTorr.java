package io.kluev.watchlist.app.downloadcontent;

import org.springframework.util.Assert;

public record EnqueuedTorr(
        String infoHash,
        String contentPath,
        Integer completionOn,
        long size
) {
    public EnqueuedTorr(String infoHash, String contentPath, Integer completionOn, long size) {
        Assert.notNull(infoHash, "infoHash cannot be null");
        Assert.notNull(contentPath, "contentPath cannot be null");
        Assert.isTrue(size >= 0, "size cannot be negative");

        this.infoHash = infoHash;
        this.contentPath = contentPath;
        this.completionOn = completionOn <= 0 ? null : completionOn;
        this.size = size;
    }

    public boolean isFinished() {
        return completionOn != null;
    }
}
