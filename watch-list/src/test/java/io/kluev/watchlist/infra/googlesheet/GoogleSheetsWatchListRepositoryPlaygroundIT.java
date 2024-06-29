package io.kluev.watchlist.infra.googlesheet;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


/**
 * This is not a real test. Just a playground with Google Sheet API
 */
@Disabled
@SuppressWarnings("unused")
@SpringBootTest(properties = {
        "integration.google.credentialsFile=/home/alex/gkeys/test_key.json",
        "integration.google.sheet.spreadsheetId=10buF2nA3Zo6sLwmoywiCs2p8MsEJc7hbjbi2Vlhfguk",
        "integration.telegramBot.enabled=false"
})
@Import(GoogleSheetProperties.class)
class GoogleSheetsWatchListRepositoryPlaygroundIT {

    @Autowired
    private Sheets service;

    @Autowired
    private GoogleSheetProperties properties;


    @SneakyThrows
    @Test
    public void should_add_movie_to_watch() {
        var googleSheetsWatchListRepository = new GoogleSheetsWatchListRepository(service, properties);
        val movieItem = MovieItem.create("Терминатор 2", 2015, "123");
        googleSheetsWatchListRepository.enlist(movieItem);
        // No exceptions are expected at this point
    }

}