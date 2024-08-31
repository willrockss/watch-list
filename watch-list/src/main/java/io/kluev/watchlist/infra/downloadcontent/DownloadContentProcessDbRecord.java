package io.kluev.watchlist.infra.downloadcontent;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record DownloadContentProcessDbRecord(
        Long id,
        String status,
        int runIteration,
        String contentItemIdentity,
        String torrFilePath,
        String torrInfoHash,
        String contentPath,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime nextRunAfter,
        byte[] context
) {
}
