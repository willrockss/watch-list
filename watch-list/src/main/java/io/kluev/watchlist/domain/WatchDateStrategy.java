package io.kluev.watchlist.domain;

import java.time.Clock;
import java.time.LocalDate;

public interface WatchDateStrategy {

    LocalDate calculateWatchDate(Clock clock);

    default LocalDate calculateWatchDate() {
        return calculateWatchDate(Clock.systemDefaultZone());
    }
}
