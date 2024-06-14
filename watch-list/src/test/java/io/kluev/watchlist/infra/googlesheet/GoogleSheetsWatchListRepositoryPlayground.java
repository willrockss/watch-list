package io.kluev.watchlist.infra.googlesheet;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


/**
 * This is not a real test. Just a playground with Google Sheet API
 */
@Disabled
@SpringBootTest(properties = {
        "integration.google.credentialsFile=/home/alex/gkeys/test_key.json",
        "integration.google.sheet.spreadsheetId=10buF2nA3Zo6sLwmoywiCs2p8MsEJc7hbjbi2Vlhfguk"
})
@Import(GoogleSheetProperties.class)
class GoogleSheetsWatchListRepositoryPlayground {

    @Autowired
    private Sheets service;

    @Autowired
    private GoogleSheetProperties properties;


    @SneakyThrows
    @Test
    public void should_add_movie_to_watch() {
        var googleSheetsWatchListRepository = new GoogleSheetsWatchListRepository(service, properties);
        googleSheetsWatchListRepository.addMovieToWatch("Терминатор 2", "Крутецкое олдовое кино", "123");
        // No exceptions are expected at this point
    }

}