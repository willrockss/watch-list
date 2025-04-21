package io.kluev.watchlist.domain.event;

import io.kluev.watchlist.domain.Series;

public record SeasonWatched(Series series, Integer seasonNumber) implements Event {
}
