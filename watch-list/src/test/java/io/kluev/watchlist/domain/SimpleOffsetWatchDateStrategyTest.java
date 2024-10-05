package io.kluev.watchlist.domain;

import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

class SimpleOffsetWatchDateStrategyTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(ZonedDateTime.parse("2022-07-25T18:30:00+03:00"), LocalDate.parse("2022-07-25")),
                Arguments.of(ZonedDateTime.parse("2022-07-25T23:59:00+03:00"), LocalDate.parse("2022-07-25")),
                Arguments.of(ZonedDateTime.parse("2022-07-26T02:59:00+03:00"), LocalDate.parse("2022-07-25")),
                Arguments.of(ZonedDateTime.parse("2022-07-26T03:00:00+03:00"), LocalDate.parse("2022-07-26")),
                Arguments.of(ZonedDateTime.parse("2022-07-26T05:00:00+03:00"), LocalDate.parse("2022-07-26")),
                Arguments.of(ZonedDateTime.parse("2022-07-26T06:00:00+04:00"), LocalDate.parse("2022-07-26"))
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void should_calculate_watch_date(ZonedDateTime now, LocalDate expectedWatchDate) {
        val sut = new SimpleOffsetWatchDateStrategy(3);
        val calculatedWatchDate = sut.calculateWatchDate(Clock.fixed(now.toInstant(), now.getZone()));
        Assertions.assertThat(expectedWatchDate).isEqualTo(calculatedWatchDate);
    }

}