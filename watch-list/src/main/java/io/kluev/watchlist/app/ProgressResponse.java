package io.kluev.watchlist.app;

public record ProgressResponse(String error) {
    public ProgressResponse() {
        this(null);
    }
}
