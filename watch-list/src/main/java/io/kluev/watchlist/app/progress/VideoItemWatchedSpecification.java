package io.kluev.watchlist.app.progress;

import io.kluev.watchlist.app.VideoType;

public interface VideoItemWatchedSpecification {

    VideoType getVideoType();
    boolean isWatched(double progress);
}
