package io.kluev.watchlist.infra.googlesheet.clientimpl;


import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Builder
@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
public final class CutInsertWithUpdateRowCommand extends GoogleSheetCommand {

    private final String sheetCodeCutFrom;
    private final RowNumber cutRowNumber;

    private final String sheetCodeInsertInto;
    private final RowNumber insertRowNumber;

    private final Map<String, Object> updatedFields;
}
