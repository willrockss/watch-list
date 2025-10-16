package io.kluev.watchlist.app.downloadcontent;

import java.util.EnumSet;
import java.util.Set;

public enum DownloadContentProcessStatus {
    INITIAL,
    PROCESSING,
    PAUSED,
    FINISHED,
    ERROR;

    public static final Set<DownloadContentProcessStatus> FINAL_STATUSES = EnumSet.of(FINISHED, ERROR);

}
