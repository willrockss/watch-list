package io.kluev.watchlist.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SeriesIdGeneratorTest {

    private final static SeriesIdGenerator sut = new SeriesIdGenerator();

    public static Stream<Arguments> generateIdWithoutChange() {
        return Stream.of(
                Arguments.of("the_good_doctor_1", "The Good Doctor", 1),
                Arguments.of("хороший_доктор_1", "Хороший Доктор", 1),
                Arguments.of("_хороший_доктор__1", "\"Хороший Доктор\"", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("generateIdWithoutChange")
    void generateIdWithoutChange(String expectedId, String rawTitle, Integer seasonNumber) {
        Assertions.assertEquals(expectedId, sut.generateId(rawTitle, seasonNumber));
    }

}