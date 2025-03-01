package io.kluev.watchlist.app;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record EnlistWatchedMovieRequest(
        String title,
        String foreignTitle,
        Integer year,
        String externalId,
        LocalDate watchedAt,
        String username
) {
}
