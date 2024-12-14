package io.kluev.watchlist.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// TODO move to another app|module later
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DownloadableContentInfo {
    @EqualsAndHashCode.Include
    String guid;
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

