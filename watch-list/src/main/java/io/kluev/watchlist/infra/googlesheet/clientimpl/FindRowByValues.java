package io.kluev.watchlist.infra.googlesheet.clientimpl;

import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@lombok.Value
public class FindRowByValues extends GoogleSheetCommand {

    String sheetCode;
    List<RowCellValue> values;
}
