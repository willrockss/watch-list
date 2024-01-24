package io.kluev.watchlist.infra;

import io.kluev.watchlist.domain.SeriesId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NodeRedSeriesId implements SeriesId {

    private final String value;
    private final Integer sheetRowNumber;

    @Override
    public String getValue() {
        return value;
    }
}
