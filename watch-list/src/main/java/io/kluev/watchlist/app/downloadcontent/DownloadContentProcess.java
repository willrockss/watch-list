package io.kluev.watchlist.app.downloadcontent;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor()
public class DownloadContentProcess {

    private final Long id;
    @Builder.Default
    private final DownloadContentProcessStatus status = DownloadContentProcessStatus.INITIAL;
    @Builder.Default
    private int runIteration = 0;
    private final ContentItemIdentity contentItemIdentity;
    private final String torrFilePath;
    private String contextInfo;
    private OffsetDateTime createdAt;
    private OffsetDateTime nextRunAfter;

}
