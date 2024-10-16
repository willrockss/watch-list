package io.kluev.watchlist.app.downloadcontent;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Objects;

@Slf4j
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
        torrInfoHash = Objects.requireNonNull(enqueuedPaused.infoHash(), () -> "hash should not be null in" + enqueuedPaused);
        contentPath = Objects.requireNonNull(enqueuedPaused.contentPath(), () -> "contentPath should not be null in" + enqueuedPaused);
        createdAt = OffsetDateTime.now();
        // TODO add history into contextInfo
    }

    public void start(QBitClient client) {
        val paused = client.findByIdTagOrNull(getContentItemIdentity());
        // TODO change status to ERROR instead of throwing exception
        Assert.notNull(paused, "Unable to find paused torr " + getContentItemIdentity());

        client.start(paused);
        status = DownloadContentProcessStatus.PROCESSING;
    }

    public boolean hasInitialStatus() {
        return status == DownloadContentProcessStatus.INITIAL;
    }

    public boolean checkFinished(QBitClient qBitClient) {
        val torr = qBitClient.findByIdTagOrNull(getContentItemIdentity());
        if (torr == null) {
            // TODO count that type of errors in the context and after sine threshold mark download as failed
            log.warn("Unable to find downloading torr by {}", getContentItemIdentity());
            return false;
        }
        if (torr.isFinished()) {
            log.info("Download {} finished", getContentItemIdentity());
            status = DownloadContentProcessStatus.FINISHED;
            return true;
        } else {
            log.debug("Download {} not finished yet", getContentItemIdentity());
        }
        return false;
    }
}
