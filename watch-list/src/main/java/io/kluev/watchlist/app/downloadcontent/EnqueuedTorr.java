package io.kluev.watchlist.app.downloadcontent;

import org.springframework.util.Assert;

public record EnqueuedTorr(
        String infoHash,
        String contentPath,
        Integer completionOn
) {
    public EnqueuedTorr(String infoHash, String contentPath, Integer completionOn) {
        Assert.notNull(infoHash, "infoHash cannot be null");
        Assert.notNull(contentPath, "contentPath cannot be null");

        this.infoHash = infoHash;
        this.contentPath = contentPath;
        this.completionOn = completionOn <= 0 ? null : completionOn;
    }
}
