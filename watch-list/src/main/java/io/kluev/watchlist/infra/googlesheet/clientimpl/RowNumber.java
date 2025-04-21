package io.kluev.watchlist.infra.googlesheet.clientimpl;

import org.springframework.util.Assert;

/**
 * 1-based Row definition = rowIndex + 1
 */
public record RowNumber(int value) {

    public static RowNumber FIRST_AFTER_HEADER = RowNumber.of(2); // first after header

    public RowNumber {
        Assert.isTrue(value > 0, "Row Number should be positive");
    }

    public static RowNumber of(int i) {
        return new RowNumber(i);
    }

    public int toRowIndex() {
        return value - 1;
    }
}
