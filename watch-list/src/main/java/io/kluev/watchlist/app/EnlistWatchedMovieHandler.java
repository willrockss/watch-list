package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class EnlistWatchedMovieHandler {

    private final MovieRepository movieRepository;

    public EnlistWatchedMovieResponse handle(EnlistWatchedMovieRequest request) {
        val movie = createMovieItemByRequest(request);
        movieRepository.enlistWatched(movie, request.watchedAt());
        return new EnlistWatchedMovieResponse(movie.getFullTitle());
    }

    private MovieItem createMovieItemByRequest(EnlistWatchedMovieRequest request) {
        if (StringUtils.isNotBlank(request.foreignTitle())) {
            return MovieItem.create(request.title(), request.foreignTitle(), request.year(), request.externalId());
        }
        return MovieItem.create(request.title(), request.year(), request.externalId());
    }
}
