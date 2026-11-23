package io.kluev.watchlist.infra.googlesheet.clientimpl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.CutPasteRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Slf4j
public class GoogleSheetsClient {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final CellFormat DATE_CELL_FORMAT = getDateStrCellFormat();
    private static final int PAGE_SIZE = 20;

    private final Sheets service;
    private final GoogleSheetProperties properties;

    private final Map<String, Integer> sheetIdByCode = new HashMap<>();

    private final Map<String, Map<String, Integer>> colNumberByCodeBySheet = new HashMap<>();

    private final Map<String, String> sheetNameByCode = new HashMap<>();
    private final Map<String, String> sheetCodeByName = new HashMap<>();

    private volatile boolean columnMappingsInitialized = false;

    public GoogleSheetsClient(
            Sheets service,
            GoogleSheetProperties properties
    ) {
        this.service = service;
        this.properties = properties;
    }

    /**
     * Sheet column mappings are loaded lazily, so temporary Google Sheets unavailability
     * does not prevent the application from starting up.
     */
    private synchronized void ensureInitialized() {
        if (columnMappingsInitialized) {
            return;
        }
        try {
            init();
            columnMappingsInitialized = true;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialize Google Sheets client", e);
        }
    }

    private void init() throws IOException {
        for (Map.Entry<String, GoogleSheetProperties.BaseSheetProperties> sheetPropBySheetCode : properties.getSheets().entrySet()) {
            var sheetCode = sheetPropBySheetCode.getKey();
            var sheetProps = sheetPropBySheetCode.getValue();
            var colNumberByCode = initSheetColumnMapping(sheetProps);
            colNumberByCodeBySheet.put(sheetCode, colNumberByCode);
            sheetNameByCode.put(sheetCode, sheetProps.getSheetName());
        }

        for (Map.Entry<String, String> stringStringEntry : sheetNameByCode.entrySet()) {
            sheetCodeByName.put(stringStringEntry.getValue(), stringStringEntry.getKey());
        }
    }

    private Map<String, Integer> initSheetColumnMapping(GoogleSheetProperties.BaseSheetProperties sheetProps) throws IOException {
        var colNumberByCode = new HashMap<String, Integer>();
        var result = service.spreadsheets().values().get(properties.getSpreadsheetId(), sheetProps.getHeaderRange().asString()).execute();
        var line = result.getValues().getFirst();
        for (int i = 0; i < line.size(); i++) {
            var value = line.get(i);
            for (Map.Entry<String, String> entry : sheetProps.getColumnsMapping().entrySet()) {
                if (Objects.equals(value, entry.getValue()))  {
                    colNumberByCode.put(entry.getKey(), i + 1);
                }
            }
        }
        return colNumberByCode;
    }

    /**
     *
     * @return rowNumber (1-based) of found row or {@code null} is nothing found
     */
    public RowNumber findRow(FindRowByValues command) {
        var sheetProp = properties.getSheets().get(command.getSheetCode());
        int rowOffset = 2; // Index to Number + Header row
        // TODO add page iteration, for now look at first page only
        GoogleSheetProperties.SheetRange range = sheetProp
                .getHeaderRange()
                .changeRowsInterval(RowNumber.of(rowOffset), RowNumber.of(rowOffset + PAGE_SIZE));
        var fullRange = range.asString();
        try {
            ensureInitialized();
            var result = service.spreadsheets().values().get(properties.getSpreadsheetId(), fullRange).execute();
            var values = result.getValues();
            if (values == null) {
                return null;
            }
            var colMapping = colNumberByCodeBySheet.get(command.getSheetCode());
            for (int i = 0; i < values.size(); i++) {
                var row = values.get(i);
                boolean allMatched = true;
                for (RowCellValue expectedValue : command.getValues()) {
                    var colNumber = colMapping.get(expectedValue.columnCode());
                    Assert.notNull(colNumber, "column number is not configured for code " + expectedValue.columnCode());
                    if (!Objects.equals(row.get(colNumber - 1), expectedValue.value())) {
                        allMatched = false;
                        break;
                    }
                }
                if (allMatched) {
                    return new RowNumber(i + rowOffset);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            log.error(
                    "Unable to fetch rows from {}, range {}. Error: {}",
                    properties.getSpreadsheetId(),
                    fullRange,
                    e.toString()
            );
        }
        return null;
    }

    public boolean deleteRow(DeleteRow command) {
        ensureInitialized();
        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest();
        request.setRequests(List.of(
                new Request()
                        .setDeleteDimension(new DeleteDimensionRequest().setRange(
                                new DimensionRange()
                                        .setSheetId(getSheetIdByCode(command.getSheetCode()))
                                        .setDimension("ROWS")
                                        .setStartIndex(command.getRowNumber().toRowIndex())
                                        .setEndIndex(command.getRowNumber().toRowIndex() + 1)
                        ))
        ));

        try {
            var resp = service.spreadsheets().batchUpdate(properties.getSpreadsheetId(), request).execute();
            return !resp.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Unable to execute " + command, e);
        }
    }

    public boolean cutInsertWithUpdate(CutInsertWithUpdateRowCommand command) {
        ensureInitialized();
        var reqList = new ArrayList<Request>();
        reqList.add(
                new Request()
                .setInsertDimension(new InsertDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(getSheetIdByCode(command.getSheetCodeInsertInto()))
                                .setDimension("ROWS")
                                .setStartIndex(command.getInsertRowNumber().toRowIndex())
                                .setEndIndex(command.getInsertRowNumber().toRowIndex() + 1))
                )
        );

        reqList.add(
                new Request()
                        .setCutPaste(
                                new CutPasteRequest()
                                        .setSource(
                                                new GridRange()
                                                        .setSheetId(getSheetIdByCode(command.getSheetCodeCutFrom()))
                                                        .setStartRowIndex(command.getCutRowNumber().toRowIndex())
                                                        .setEndRowIndex(command.getCutRowNumber().toRowIndex() + 1)
                                        )
                                        .setDestination(
                                                new GridCoordinate()
                                                        .setSheetId(getSheetIdByCode(command.getSheetCodeInsertInto()))
                                                        .setRowIndex(command.getInsertRowNumber().toRowIndex())
                                        )


                        )
        );
        reqList.addAll(generateUpdateRequests(command));
        reqList.add(new Request()
                .setDeleteDimension(new DeleteDimensionRequest().setRange(
                        new DimensionRange()
                                .setSheetId(getSheetIdByCode(command.getSheetCodeCutFrom()))
                                .setDimension("ROWS")
                                .setStartIndex(command.getCutRowNumber().toRowIndex())
                                .setEndIndex(command.getCutRowNumber().toRowIndex() + 1)
                ))
        );
        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest();
        request.setRequests(reqList);

        final BatchUpdateSpreadsheetResponse resp;
        try {
            resp = service.spreadsheets().batchUpdate(properties.getSpreadsheetId(), request).execute();
            return !resp.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Unable to perform " + command, e);
        }
    }

    private List<Request> generateUpdateRequests(CutInsertWithUpdateRowCommand command) {
        if (command.getUpdatedFields() == null || command.getUpdatedFields().isEmpty()) {
            return List.of();
        }

        // TODO Group by adjacent cell
        var result = new ArrayList<Request>();
        var sheetId = getSheetIdByCode(command.getSheetCodeInsertInto());
        var rowIndex = command.getInsertRowNumber().toRowIndex();

        Map<String, Integer> colNumberByCode = requireNonNull(colNumberByCodeBySheet.get(command.getSheetCodeInsertInto()),
                () -> "Column mapping is not defined for sheet [" + command.getSheetCodeInsertInto() + "]");

        for (Map.Entry<String, Object> colCodeValuePair : command.getUpdatedFields().entrySet()) {
            var colCode = colCodeValuePair.getKey();
            var colNumber = requireNonNull(colNumberByCode.get(colCode),
                    () -> "columnNumber mapping is not specified for column " +
                            "[" + colCode + "] for sheet [" + command.getSheetCodeInsertInto() + "]");
            var colIndex = colNumber - 1;
            var value = colCodeValuePair.getValue();
           result.add(generateUpdateRequestForSingleValue(sheetId, rowIndex, colIndex, value));
        }
        return result;
    }

    private Request generateUpdateRequestForSingleValue(
            @NonNull Integer sheetId,
            @NonNull Integer rowIndex,
            @NonNull Integer columnIndex,
            @Nullable Object value
    ) {
        return new Request()
                .setUpdateCells(
                        new UpdateCellsRequest()
                                .setStart(new GridCoordinate()
                                        .setSheetId(sheetId)
                                        .setRowIndex(rowIndex)
                                        .setColumnIndex(columnIndex))
                                .setRows(List.of(
                                        new RowData().setValues(
                                                List.of(
                                                        getCellDataForValue(value)
                                                )
                                        )
                                ))
                                .setFields("*")
                );
    }

    private CellData getCellDataForValue(@Nullable Object value) {
        if (value == null) {
            return new CellData();
        }

        var extVal = new ExtendedValue();
        switch (value) {
            case Number d -> extVal.setNumberValue(d.doubleValue());
            case Boolean b -> extVal.setBoolValue(b);
            case String s -> extVal.setStringValue(s);
            case LocalDate ld -> extVal.setStringValue(DATE_FORMATTER.format(ld));
            default -> extVal.setStringValue(value.toString());
        }

        var cellData = new CellData().setUserEnteredValue(extVal);

        if (value instanceof LocalDate) {
            cellData.setUserEnteredFormat(DATE_CELL_FORMAT);
        }
        return cellData;
    }

    @SneakyThrows
    private Integer getSheetIdByCode(@NotNull String sheetCode) {
        if (sheetIdByCode.isEmpty()) {
            Spreadsheet spreadsheet = service.spreadsheets().get(properties.getSpreadsheetId()).execute();
            for (Sheet sheet : spreadsheet.getSheets()) {
                var props = requireNonNull(sheet.getProperties(), () -> "Properties is null for " + sheet);
                var code = this.sheetCodeByName.get(props.getTitle());
                sheetIdByCode.put(code, props.getSheetId());
            }
        }
        return requireNonNull(sheetIdByCode.get(sheetCode), () -> "Sheet not found by " + sheetCode);
    }

    private static CellFormat getDateStrCellFormat() {
        var cellFormat = new CellFormat();
        cellFormat.setHorizontalAlignment("RIGHT");
        return cellFormat;
    }
}
