package io.kluev.watchlist.infra.googlesheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadFinishedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadStartedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemEnqueuedEvent;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
public class GoogleSheetsWatchListRepository implements MovieRepository {

    private final Sheets service;
    private final GoogleSheetProperties properties;

    private final String insertRange;

    public GoogleSheetsWatchListRepository(Sheets service, GoogleSheetProperties properties) {
        this.service = service;
        this.properties = properties;
        this.insertRange = calculateInsertRange();
    }

    private String calculateInsertRange() {
        val headerRange = properties.getMoviesToWatch().getHeaderRange();
        int delimiterIndex = headerRange.indexOf("!");
        return headerRange.substring(0, delimiterIndex + 1) + "A2:A2";
    }

    @SneakyThrows
    @Override
    public void enlist(MovieItem movieItem) {
        val preComment = "";
        service.spreadsheets().values().append(
                        properties.getSpreadsheetId(),
                        insertRange,
                        new ValueRange()
                                .setValues(List.of(List.of(
                                        movieItem.getFullTitle(), "", preComment, "", "", "", "", "", "", movieItem.getExternalId()
                                )))
                                .setMajorDimension("ROWS")
                )
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    @Override
    public List<MovieItem> getMoviesToWatch() {
        val toWatchMoviesRows = findToWatchMoviesRows();
        return toWatchMoviesRows.stream()
                .map(this::mapToMovieItemOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private @NotNull List<List<Object>> findToWatchMoviesRows() {
        val range = "'Посмотреть'!A2:N22"; // TODO read from properties
        try {
            val result = service.spreadsheets().values().get(properties.getSpreadsheetId(), range).execute();
            val values = result.getValues();
            if (values == null) {
                return List.of();
            }
            return values;
        } catch (IOException e) {
            log.error(
                    "Unable to fetch first 20 rows from {}, range {}. Error: {}",
                    properties.getSpreadsheetId(),
                    range,
                    e.toString()
            );
        }
        return List.of();
    }

    private @Nullable MovieItem mapToMovieItemOrNull(List<Object> row) {
        return new MovieItemRowMapper(row).toMovieItemOrNull();
    }

    @SneakyThrows
    public void addWatched(String fullTitle, String postComment, String kinopoiskId) {

        var rowData = IntStream
                .rangeClosed(0, 9) // TODO Make column indexes configurable
                .mapToObj(i -> switch (i) {
                    case 0 -> new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(fullTitle));
                    case 1 -> new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(
                            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                    ));
                    case 5 -> new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(postComment));
                    case 9 -> new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(
                            Double.parseDouble(kinopoiskId)
                    ));
                    default -> new CellData();
                })
                .toList();

        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest();
        request.setRequests(List.of(
                new Request()
                        .setInsertDimension(new InsertDimensionRequest()
                                .setRange(new DimensionRange()
                                        .setSheetId(1)  // TODO Make sheet id configurable
                                        .setDimension("ROWS")
                                        .setStartIndex(1)  // Insert one empty row at the beginning
                                        .setEndIndex(2))
                        ),
                new Request()
                        .setUpdateCells(
                                new UpdateCellsRequest()
                                        .setStart(new GridCoordinate()
                                                .setSheetId(0)  // Assuming the first sheet in the spreadsheet
                                                .setRowIndex(1)  // Insert at the beginning
                                                .setColumnIndex(0))
                                        .setRows(List.of(
                                                new RowData().setValues(rowData)
                                        ))
                                        .setFields("*")
                        )
        ));

        service.spreadsheets().batchUpdate(properties.getSpreadsheetId(), request).execute();
    }

    @EventListener(ContentItemEnqueuedEvent.class)
    public void markAsEnqueued(ContentItemEnqueuedEvent event) throws IOException {
        val itemIndex = findRowByItemKinopoiskIdOrNull(event.identity().value());
        if (itemIndex == null) {
            log.error("Unable to find row by {}. Do nothing", event.identity());
            return;
        }
        updateStatus(itemIndex, "ENQUEUED");
    }

    @EventListener(ContentItemDownloadStartedEvent.class)
    public void markAsStarted(ContentItemDownloadStartedEvent event) throws IOException {
        val itemIndex = findRowByItemKinopoiskIdOrNull(event.identity().value());
        if (itemIndex == null) {
            log.error("Unable to find row by {}. Do nothing", event.identity());
            return;
        }
        updateStatusAndContentPath(itemIndex, "DOWNLOADING", event.contentPath());
    }

    @EventListener(ContentItemDownloadFinishedEvent.class)
    public void markAsFinished(ContentItemDownloadFinishedEvent event) throws IOException {
        val itemIndex = findRowByItemKinopoiskIdOrNull(event.identity().value());
        if (itemIndex == null) {
            log.error("Unable to find row by {}. Do nothing", event.identity());
            return;
        }
        updateStatus(itemIndex, "READY");
    }

    /**
     * First we need to find raw by identifier (kinopoisk id for now)
     * Since there is no direct way to do so in Google Sheets API v4 we just read first 20 rows and
     * try to find among them.
     * @return row index. 1 is the first row
     */
    private Integer findRowByItemKinopoiskIdOrNull(@NotNull String kinopoiskId) {
        Assert.notNull(kinopoiskId, "kinopoiskId cannot be null");

        val range = "'Посмотреть'!J2:J22"; // TODO read from properties
        try {
            val result = service.spreadsheets().values().get(properties.getSpreadsheetId(), range).execute();
            val values = result.getValues();
            if (values == null) {
                return null;
            }
            for(int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.isEmpty()) {
                    continue;
                }
                if (kinopoiskId.equals(row.getFirst())) {
                    return i + 2; // TODO read from properties
                }
            }
        } catch (IOException e) {
            log.error(
                    "Unable to fetch first 20 rows from {}, range {}. Error: {}",
                    properties.getSpreadsheetId(),
                    range,
                    e.toString()
            );
        }
        return null;
    }

    private void updateStatus(int rowIndex, String status) throws IOException {
        val statusRange = "Посмотреть!K" + rowIndex + ":K" + rowIndex; // TODO read from properties
        update(statusRange, List.of(status));
    }

    private void updateStatusAndContentPath(int rowIndex, String status, String contentPath) throws IOException {
        val range = "Посмотреть!K" + rowIndex + ":L" + rowIndex; // TODO read from properties
        update(range, List.of(status, contentPath));
    }

    private void update(String range, List<Object> values) throws IOException {
        val rawResponse = service.spreadsheets().values().update(
                        properties.getSpreadsheetId(),
                        range,
                        new ValueRange()
                                .setValues(List.of(values))
                                .setMajorDimension("ROWS")

                                .setRange(range)
                )
                .setValueInputOption("USER_ENTERED")
                .execute();
        log.debug("Updated data: {}", rawResponse.getUpdatedData());
    }
}
