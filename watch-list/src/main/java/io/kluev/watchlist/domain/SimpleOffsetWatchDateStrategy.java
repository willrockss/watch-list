package io.kluev.watchlist.domain;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * If you watch movie at night (let's say from Friday to Saturday) movie would be marked as watched at Friday
 * according to the {@link #offsetHours} property.
 */
@RequiredArgsConstructor
public class SimpleOffsetWatchDateStrategy implements WatchDateStrategy{

    private final int offsetHours;

    @Override
    public LocalDate calculateWatchDate(Clock clock) {
        val now = ZonedDateTime.now(clock);
        val nowWithOffset = now.minusHours(offsetHours);
        return nowWithOffset.toLocalDate();
    }
}
