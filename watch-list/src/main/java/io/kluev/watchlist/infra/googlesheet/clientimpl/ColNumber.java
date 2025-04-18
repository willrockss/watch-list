package io.kluev.watchlist.infra.googlesheet.clientimpl;

import org.springframework.util.Assert;

/**
 * 1-based Column definition = rowIndex + 1
 */
public record ColNumber(int value) {
    public ColNumber {
        Assert.isTrue(value > 0, "Column Number should be positive");
    }

    public static ColNumber of(int i) {
        return new ColNumber(i);
    }

    public int toRowIndex() {
        return value - 1;
    }
}
