package io.kluev.watchlist.infra;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


class NodeRedSeriesIdTest {

    @Test
    void testToString() {
        // given
        NodeRedSeriesId id = new NodeRedSeriesId("волга_1", 15);

        // when
        String result = id.toString();

        // then
        Assertions.assertThat(result).isEqualTo("NodeRedSeriesId{волга_1}");
    }
}