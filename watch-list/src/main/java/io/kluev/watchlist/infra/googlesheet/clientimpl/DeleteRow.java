package io.kluev.watchlist.infra.googlesheet.clientimpl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeleteRow extends GoogleSheetCommand {
    String sheetCode;
    RowNumber rowNumber;

    public static DeleteRow of(String sheetCode, RowNumber rowNumber) {
        return new DeleteRow(sheetCode, rowNumber);
    }
}
