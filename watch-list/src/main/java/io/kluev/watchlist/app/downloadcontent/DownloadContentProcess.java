package io.kluev.watchlist.app.downloadcontent;


import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Objects;

@Slf4j
@ToString
@Getter
@Builder
public class DownloadContentProcess {

    private Long id;
    @Builder.Default
    private DownloadContentProcessStatus status = DownloadContentProcessStatus.INITIAL;
    @Builder.Default
    private int runIteration = 0;
    private final ContentItemIdentity contentItemIdentity;
    /** Path to a local .torrent file; mutually exclusive with {@link #link}. */
    private final String torrFilePath;
    /** Magnet {@code magnet:?xt=...} or http(s) link to a torrent; mutually exclusive with {@link #torrFilePath}. */
    private final String link;
    private String torrInfoHash;
    private String contentPath;
    private String contextInfo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime nextRunAfter;

    /**
     * All-args constructor used by Lombok's {@code @Builder}. Validates that exactly one of
     * {@code torrFilePath} and {@code link} is set.
     */
    public DownloadContentProcess(Long id,
                                  DownloadContentProcessStatus status,
                                  int runIteration,
                                  ContentItemIdentity contentItemIdentity,
                                  String torrFilePath,
                                  String link,
                                  String torrInfoHash,
                                  String contentPath,
                                  String contextInfo,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt,
                                  OffsetDateTime nextRunAfter) {
        Assert.isTrue((torrFilePath == null) ^ (link == null),
                () -> "Exactly one of torrFilePath or link must be set, but got torrFilePath=" + torrFilePath
                        + " and link=" + link);
        this.id = id;
        this.status = status == null ? DownloadContentProcessStatus.INITIAL : status;
        this.runIteration = runIteration;
        this.contentItemIdentity = contentItemIdentity;
        this.torrFilePath = torrFilePath;
        this.link = link;
        this.torrInfoHash = torrInfoHash;
        this.contentPath = contentPath;
        this.contextInfo = contextInfo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nextRunAfter = nextRunAfter;
    }

    public void enqueuePaused(QBitClient client) {
        var enqueuedPaused = torrFilePath != null
                ? client.addTorrByFilePaused(torrFilePath, contentItemIdentity)
                : client.addTorrByLinkPaused(link, contentItemIdentity);

        status = DownloadContentProcessStatus.PAUSED;
        torrInfoHash = Objects.requireNonNull(enqueuedPaused.infoHash(), () -> "hash should not be null in" + enqueuedPaused);
        // For magnet links addTorrByLinkPaused waits for the metadata fetch first, so the
        // content path is already known here. A null path is a hard error: the torrent is
        // treated as not enqueued (the process stays INITIAL and is retried on the next tick).
        contentPath = Objects.requireNonNull(enqueuedPaused.contentPath(), () -> "contentPath should not be null in" + enqueuedPaused);
        createdAt = OffsetDateTime.now();
        // TODO add history into contextInfo
    }

    public void start(QBitClient client) {
        var paused = client.findByIdTagOrNull(getContentItemIdentity());
        // TODO change status to ERROR instead of throwing exception
        Assert.notNull(paused, "Unable to find paused torr " + getContentItemIdentity());

        client.start(paused);
        status = DownloadContentProcessStatus.PROCESSING;
    }

    public boolean hasInitialStatus() {
        return status == DownloadContentProcessStatus.INITIAL;
    }

    public boolean checkFinished(QBitClient qBitClient) {
        var torr = qBitClient.findByIdTagOrNull(getContentItemIdentity());
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
