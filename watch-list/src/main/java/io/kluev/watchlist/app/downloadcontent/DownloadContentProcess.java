package io.kluev.watchlist.app.downloadcontent;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;

@ToString
@Getter
@Builder
@AllArgsConstructor()
public class DownloadContentProcess {

    private Long id;
    @Builder.Default
    private DownloadContentProcessStatus status = DownloadContentProcessStatus.INITIAL;
    @Builder.Default
    private int runIteration = 0;
    private final ContentItemIdentity contentItemIdentity;
    private final String torrFilePath;
    private String torrInfoHash;
    private String contentPath;
    private String contextInfo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime nextRunAfter;

    public void enqueuePaused(QBitClient client) {
        val enqueuedPaused = client.addTorrPaused(getTorrFilePath(), getContentItemIdentity());

        status = DownloadContentProcessStatus.PAUSED;
        torrInfoHash = enqueuedPaused.infoHash();
        contentPath = enqueuedPaused.contentPath();
        createdAt = OffsetDateTime.now();
        // TODO add history into contextInfo
    }

    public void start(QBitClient client) {
        val paused = client.findByIdTagOrNull(getContentItemIdentity());
        Assert.notNull(paused, "Unable to find paused torr " + getContentItemIdentity());

        client.start(paused);
        status = DownloadContentProcessStatus.PROCESSING;
    }

    public boolean hasInitialStatus() {
        return status == DownloadContentProcessStatus.INITIAL;
    }
}
