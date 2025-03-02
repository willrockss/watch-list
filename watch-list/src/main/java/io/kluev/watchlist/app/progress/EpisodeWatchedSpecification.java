package io.kluev.watchlist.app.progress;

import io.kluev.watchlist.app.VideoType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EpisodeWatchedSpecification implements VideoItemWatchedSpecification {

    private final double watchProgressThresholdPercent;

    @Override
    public VideoType getVideoType() {
        return VideoType.EPISODE;
    }

    @Override
    public boolean isWatched(double progress) {
        return progress > watchProgressThresholdPercent;
    }
}
