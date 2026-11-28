package io.kluev.watchlist.infra.jacred.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kluev.watchlist.app.DownloadableContentInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TorrentListItem {
    private String tracker;
    private String title;
    private Long size;
    private String sizeName;
    private String url;
    private String magnet;
    private Integer sid;
    private Integer pir;
    private String createTime;
    private String updateTime;
    private String name;
    private String originalname;
    private Integer relased;
    private String videotype;
    private Integer quality;
    private List<String> voices;
    private List<Integer> seasons;

    public DownloadableContentInfo toDownloadableContentInfo() {
        // JacRed has no download counter, so downloadedTimes stays null.
        // Both link (direct .torrent url) and magnet (raw magnet URI) are filled; either can
        // be null. guid keeps the url-with-magnet-fallback identity.
        return DownloadableContentInfo.builder()
                .guid(url != null ? url : magnet)
                .title(title)
                .link(url)
                .magnet(magnet)
                .size(size)
                .statistics(new DownloadableContentInfo.Statistics(null, sid, pir))
                .build();
    }
}
