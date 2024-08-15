package io.kluev.watchlist.app.downloadcontent;

import java.util.Set;

public enum DownloadContentProcessStatus {
    INITIAL,
    PROCESSING,
    FINISHED,
    ERROR;

    public static final Set<DownloadContentProcessStatus> FINAL_STATUSES = Set.of(FINISHED, ERROR);

}