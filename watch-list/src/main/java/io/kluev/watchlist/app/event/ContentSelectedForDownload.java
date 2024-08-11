package io.kluev.watchlist.app.event;

import io.kluev.watchlist.domain.MovieItem;

public record ContentSelectedForDownload(
        MovieItem movieItem,
        String torrFilename
) {

}
