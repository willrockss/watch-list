package io.kluev.watchlist.domain;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * If you watch movie at night (let's say from Friday to Saturday) movie would be marked as watched at Friday
 * according to the {@link #offsetHours} property.
 */
@RequiredArgsConstructor
public class SimpleOffsetWatchDateStrategy implements WatchDateStrategy {

    private final int offsetHours;

    @Override
    public LocalDate calculateWatchDate(Clock clock) {
        var now = ZonedDateTime.now(clock);
        var nowWithOffset = now.minusHours(offsetHours);
        return nowWithOffset.toLocalDate();
    }
}
