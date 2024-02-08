package io.kluev.watchlist.infra;

import io.kluev.watchlist.domain.SeriesId;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
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
