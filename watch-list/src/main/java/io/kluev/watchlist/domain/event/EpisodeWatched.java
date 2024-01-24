package io.kluev.watchlist.domain.event;

import io.kluev.watchlist.domain.Episode;

public record EpisodeWatched(Episode episode) implements Event {
}
