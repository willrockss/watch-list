package io.kluev.watchlist.infra.googlesheet.clientimpl;

public record RowCellValue(
        String columnCode,
        String value
) {
}
