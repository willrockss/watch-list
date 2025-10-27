package io.kluev.watchlist.app.progress;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProgressResponse(
        String error,
        Boolean isMarkedAsWatched
) {
    public ProgressResponse() {
        this(null, null);
    }

    public static ProgressResponse ok() {
        return new ProgressResponse();
    }

    public static ProgressResponse watched() {
        return new ProgressResponse(null, true);
    }

    public static ProgressResponse error(String error) {
        return new ProgressResponse(error, null);
    }
}
