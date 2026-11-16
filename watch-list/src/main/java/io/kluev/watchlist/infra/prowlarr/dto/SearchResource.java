package io.kluev.watchlist.infra.prowlarr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kluev.watchlist.app.DownloadableContentInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResource {
    private String guid;
    private String title;
    private String downloadUrl;
    private String magnetUrl;
    private Long size;
    private Integer seeders;
    private Integer leechers;
    private Integer grabs;
    private String infoHash;
    private String indexer;
    private String protocol;

    public DownloadableContentInfo toDownloadableContentInfo() {
        return DownloadableContentInfo.builder()
                .guid(guid)
                .title(title)
                .link(downloadUrl != null ? downloadUrl : magnetUrl)
                .size(size)
                // some indexers don't report grabs at all, default to 0 to keep statistics rendering safe
                .statistics(new DownloadableContentInfo.Statistics(grabs != null ? grabs : 0, seeders, leechers))
                .build();
    }
}
