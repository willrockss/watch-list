package io.kluev.watchlist.app;

public record PlayResponse(String error) {
    public PlayResponse() {
        this(null);
    }
}
