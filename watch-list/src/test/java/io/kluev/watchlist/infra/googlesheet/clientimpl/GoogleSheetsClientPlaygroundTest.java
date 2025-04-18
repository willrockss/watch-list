package io.kluev.watchlist.infra.googlesheet.clientimpl;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.infra.config.beans.GoogleSheetServiceConfig;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@Tag("IntegrationTest")
@SpringBootTest(
        classes = {
                GoogleSheetsClient.class
        },
        properties = {
                "integration.google.credentialsFile=/home/alex/gkeys/test_key.json",
                "integration.google.spreadsheet.spreadsheetId=1agl6pPZIGwhMmS8rkkewp38DZl7TvcU0-UeyszjfIeQ"
        }
)
@Import({GoogleSheetServiceConfig.class, GoogleSheetsClientPlaygroundTest.TestConfig.class})
@EnableConfigurationProperties(value = GoogleSheetProperties.class)
class GoogleSheetsClientPlaygroundTest {

    @Autowired
    private GoogleSheetsClient client;

    @Test
    public void ok_find() {
        val result = client.findRow(new FindRowByValues(
                "series",
                List.of(
                        new RowCellValue("name", "Волга"),
                        new RowCellValue("seasonNumber", "1")
                )
        ));
        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo(3);
    }

    @Test
    public void ok_delete() {
        val result = client.deleteRow(DeleteRow.of("watchedMovies", RowNumber.of(2)));
        assertThat(result).isTrue();
    }

    @Test
    public void ok_find_and_delete() {
        val sheetCode = "series";
        val rowNumberToDelete = client.findRow(new FindRowByValues(
                sheetCode,
                List.of(
                        new RowCellValue("name", "Волга"),
                        new RowCellValue("seasonNumber", "1")
                )
        ));

        val result = client.deleteRow(DeleteRow.of(sheetCode, rowNumberToDelete));
        assertThat(result).isTrue();
    }

    @Test
    public void ok_cut_insert_with_modification() throws IOException {
        val result = client.cutInsertWithUpdate(
                CutInsertWithUpdateRowCommand
                        .builder()
                        .sheetCodeCutFrom("moviesToWatch")
                        .cutRowNumber(RowNumber.of(4))
                        .sheetCodeInsertInto("watchedMovies")
                        .insertRowNumber(RowNumber.of(2))
                        .updatedFields(
                                Map.of(
                                        "postComment", "СПМ Отличный фильм",
                                        "watchedAt", LocalDate.now().toString()
                                )
                        )
                        .build()
        );
        assertThat(result).isTrue();
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        public GoogleSheetsClient client(Sheets service, GoogleSheetProperties props) {
            return new GoogleSheetsClient(service, props);
        }
    }
}