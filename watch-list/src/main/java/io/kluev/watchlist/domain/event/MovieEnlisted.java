package io.kluev.watchlist.domain.event;

import io.kluev.watchlist.domain.MovieItem;

public record MovieEnlisted(
    MovieItem movie,
    String enlistedBy
) implements Event {
}
