package io.kluev.watchlist.app.progress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kluev.watchlist.app.VideoType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProgressRequestV2(
        VideoType videoType,
        String videoId,
        String videoPath,
        Float progress,
        Boolean sendNotification
) {
}
