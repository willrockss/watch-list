package io.kluev.watchlist.infra.googlesheet;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadFinishedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadStartedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemEnqueuedEvent;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.config.beans.GoogleSheetServiceConfig;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;


/**
 * This is not a real test. Just a playground with Google Sheet API
 */
@Disabled
@SuppressWarnings("unused")
@Tag("IntegrationTest")
@EnableConfigurationProperties(value = GoogleSheetProperties.class)
@SpringBootTest(
        classes = {
            GoogleSheetProperties.class
        },
        properties = {
            "integration.google.credentialsFile=/home/alex/gkeys/test_key.json",
            "integration.google.spreadsheet.spreadsheetId=1agl6pPZIGwhMmS8rkkewp38DZl7TvcU0-UeyszjfIeQ"
        }
)
@Import(GoogleSheetServiceConfig.class)
class WatchListRepositoryPlaygroundIT {

    @Autowired
    private Sheets service;

    @Qualifier("googleSheetProperties")
    @Autowired
    private GoogleSheetProperties properties;

    private MovieRepository movieRepository;

    @BeforeEach
    public void init() {
        movieRepository = new GoogleSheetsWatchListRepository(service, properties);
    }

    @SneakyThrows
    @Test
    public void should_add_movie_to_watch() {
        val movieItem = MovieItem.create("Терминатор 2", 2015, "123");
        movieRepository.enlist(movieItem);
        // No exceptions are expected at this point
    }

    @SneakyThrows
    @Test
    public void should_mark_movie_as_watched() {
        movieRepository.markWatched("123", LocalDate.now());
    }


    @SneakyThrows
    @Test
    public void should_update_status() {
        movieRepository.markAsEnqueued(new ContentItemEnqueuedEvent(new ContentItemIdentity("7510")));
    }

    @SneakyThrows
    @Test
    public void should_update_status_and_content_path() {
        movieRepository.markAsStarted(new ContentItemDownloadStartedEvent(new ContentItemIdentity("7510"), "/content/path"));
    }

    @SneakyThrows
    @Test
    public void should_update_status_ready() {
        movieRepository.markAsFinished(new ContentItemDownloadFinishedEvent(new ContentItemIdentity("7510")));
    }

    @SneakyThrows
    @Test
    public void should_get_movie_list() {
        val result = movieRepository.getMoviesToWatch();
        System.out.println(result);
    }

    @SneakyThrows
    @Test
    public void should_add_watched_movie() {
        val movieItem = MovieItem.create("Терминатор 2", 2015, "123");
        movieRepository.enlistWatched(movieItem, LocalDate.now());
        // No exceptions are expected at this point
    }

}