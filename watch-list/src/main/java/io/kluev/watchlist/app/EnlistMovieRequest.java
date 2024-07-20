package io.kluev.watchlist.app;

import lombok.Builder;

@Builder
public record EnlistMovieRequest(
        String title,
        String foreignTitle,
        Integer year,
        String externalId,
        String username
) {
}
