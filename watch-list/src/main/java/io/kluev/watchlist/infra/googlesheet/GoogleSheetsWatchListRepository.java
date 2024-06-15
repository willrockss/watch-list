package io.kluev.watchlist.infra.googlesheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import lombok.SneakyThrows;
import lombok.val;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

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
}
