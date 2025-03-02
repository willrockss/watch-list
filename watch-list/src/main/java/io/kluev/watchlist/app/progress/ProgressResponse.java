package io.kluev.watchlist.app.progress;

public record ProgressResponse(String error) {
    public ProgressResponse() {
        this(null);
    }
}
