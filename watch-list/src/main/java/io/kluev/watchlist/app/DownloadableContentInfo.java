package io.kluev.watchlist.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO move to another app|module later
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadableContentInfo {
    String title;
    String link;
    Long size;
    Statistics statistics;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
            Integer downloadedTimes;
            Integer seeders;
            Integer peers;
    }
}

