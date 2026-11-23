package io.kluev.watchlist.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kluev.watchlist.domain.MovieItem;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: MovieItem must be deserializable by the Temporal data converter
 * (Jackson 2), which is used to encode activity results such as {@link EnlistMovieResponse}.
 */
class EnlistMovieResponseSerializationTest {

    private final DataConverter converter = DefaultDataConverter.STANDARD_INSTANCE;

    @Test
    void should_round_trip_enlist_movie_response_through_temporal_data_converter() {
        var original = new EnlistMovieResponse(
                MovieItem.builder()
                        .title("Inception")
                        .foreignTitle("Начало")
                        .year(2010)
                        .externalId("ext-1")
                        .status("READY")
                        .filePath("/movies/inception.mkv")
                        .build(),
                true);

        var payloads = converter.toPayloads(original);
        var restored = converter.fromPayloads(0, payloads, EnlistMovieResponse.class, EnlistMovieResponse.class);

        assertThat(restored.justAdded()).isTrue();
        assertThat(restored.movieItem().getTitle()).isEqualTo("Inception");
        assertThat(restored.movieItem().getForeignTitle()).isEqualTo("Начало");
        assertThat(restored.movieItem().getYear()).isEqualTo(2010);
        assertThat(restored.movieItem().getExternalId()).isEqualTo("ext-1");
        assertThat(restored.movieItem().getStatus()).isEqualTo("READY");
        assertThat(restored.movieItem().getFilePath()).isEqualTo("/movies/inception.mkv");
        assertThat(restored.movieItem().isReady()).isTrue();
    }

    @Test
    void should_ignore_unknown_properties() throws Exception {
        var mapper = new ObjectMapper();

        var item = mapper.readValue("{\"title\":\"T\",\"unknownField\":\"x\"}", MovieItem.class);

        assertThat(item.getTitle()).isEqualTo("T");
    }
}
