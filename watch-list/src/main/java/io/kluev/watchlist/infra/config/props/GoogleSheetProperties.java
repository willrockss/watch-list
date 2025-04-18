package io.kluev.watchlist.infra.config.props;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.kluev.watchlist.infra.googlesheet.clientimpl.ColNumber;
import io.kluev.watchlist.infra.googlesheet.clientimpl.RowNumber;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "integration.google.spreadsheet")
public class GoogleSheetProperties {
    public static final String MOVIES_TO_WATCH = "moviesToWatch";
    public static final String WATCHED_MOVIES = "watchedMovies";
    public static final String SERIES = "series";

    private String spreadsheetId;

    @NotNull
    private Map<String, GoogleSheetProperties.BaseSheetProperties> sheets;

    public BaseSheetProperties getMoviesToWatch() {
        return sheets.get(MOVIES_TO_WATCH);
    }

    public BaseSheetProperties getWatchedMovies() {
        return sheets.get(WATCHED_MOVIES);
    }

    public BaseSheetProperties getSeries() {
        return sheets.get(SERIES);
    }

    @Data
    public static class BaseSheetProperties {
        private SheetRange headerRange;
        private Map<String, String> columnsMapping;

        @JsonIgnore
        public String getSheetName() {
            return headerRange.getSheetName();
        }

    }

    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SheetRange {

        private static final Pattern CELL_PATTERN = Pattern.compile("^R(\\d+)C(\\d+)$");

        private final String sheetName;
        private final RowNumber fromRow;
        private final ColNumber fromCol;
        private final RowNumber toRow;
        private final ColNumber toCol;

        public SheetRange(String rawValue) {
            int delimiterIndex = rawValue.indexOf('!');
            this.sheetName = rawValue.substring(0, delimiterIndex);

            String[] parts = rawValue
                    .substring(delimiterIndex + 1)
                    .split(":");

            Assert.isTrue(parts.length == 2, "Input string must contain exactly two cells separated by a colon.");

            int[] from = parseCell(parts[0]);
            int[] to = parseCell(parts[1]);

            this.fromRow = RowNumber.of(from[0]);
            this.fromCol = ColNumber.of(from[1]);
            this.toRow = RowNumber.of(to[0]);
            this.toCol = ColNumber.of(to[1]);
        }

        public SheetRange changeRowsInterval(RowNumber fromRowNumber, RowNumber toRowNumber) {
            return this.toBuilder()
                    .fromRow(fromRowNumber)
                    .toRow(toRowNumber)
                    .build();
        }

        private static int[] parseCell(String cell) {
            Matcher matcher = CELL_PATTERN.matcher(cell);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid cell format: " + cell);
            }

            int row = Integer.parseInt(matcher.group(1));
            int col = Integer.parseInt(matcher.group(2));
            return new int[]{row, col};
        }

        public String asString() {
            return "'%s'!R%dC%d:R%dC%d".formatted(sheetName, fromRow.value(), fromCol.value(), toRow.value(), toCol.value());
        }
    }

}


