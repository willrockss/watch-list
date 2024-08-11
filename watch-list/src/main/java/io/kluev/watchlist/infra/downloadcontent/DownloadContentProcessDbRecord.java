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
        OffsetDateTime createdAt,
        OffsetDateTime nextRunAfter,
        byte[] context
) {
}
