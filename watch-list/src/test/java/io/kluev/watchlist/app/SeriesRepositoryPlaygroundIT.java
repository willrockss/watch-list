package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesId;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.SimpleOffsetWatchDateStrategy;
import io.kluev.watchlist.domain.WatchDateStrategy;
import io.kluev.watchlist.infra.CompositeSeriesRepository;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.config.beans.GoogleSheetServiceConfig;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsSeriesRepository;
import io.kluev.watchlist.infra.googlesheet.clientimpl.GoogleSheetsClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@Tag("IntegrationTest")
@EnableConfigurationProperties(value = GoogleSheetProperties.class)
@SpringBootTest(
        classes = {
                GoogleSheetsClient.class,
                GoogleSheetsSeriesRepository.class,
        },
        properties = {
                "integration.google.credentialsFile=/home/alex/gkeys/test_key.json",
                "integration.google.spreadsheet.spreadsheetId=1agl6pPZIGwhMmS8rkkewp38DZl7TvcU0-UeyszjfIeQ"
        }
)
@Import({GoogleSheetServiceConfig.class, SeriesRepositoryPlaygroundIT.TestConfig.class})
class SeriesRepositoryPlaygroundIT {

    @MockBean
    private NodeRedSeriesRepository nodeRedSeriesRepository;

    @Autowired
    private GoogleSheetsSeriesRepository googleSheetsSeriesRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @Test
    public void ok_mark_season_as_watched() {
        String seasonId = new SeriesIdGenerator().generateId("Волга", 2);
        val season = new Series(
                new TestSeriesId(seasonId),
                "Волга",
                "Волга (1, 1/2)",
                Path.of("/tmp"),
                2,
                1
        );
        season.getEpisodes().add(new Episode(1, "e01.mov"));
        season.getEpisodes().add(new Episode(2, "e02.mov"));

        boolean result = season.markEpisodeWatched("e02.mov");
        assertThat(result).isTrue();

        seriesRepository.save(season);
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        public SeriesRepository seriesRepository(
                NodeRedSeriesRepository nodeRedSeriesRepository,
                GoogleSheetsSeriesRepository googleSheetsSeriesRepository
        ) {

            return new CompositeSeriesRepository(
                    nodeRedSeriesRepository,
                    googleSheetsSeriesRepository
            );
        }

        @Bean
        public WatchDateStrategy watchDateStrategy() {
            return new SimpleOffsetWatchDateStrategy(3);
        }

    }

    @AllArgsConstructor
    @Getter
    public static class TestSeriesId implements SeriesId {

        private final String value;
    }
}