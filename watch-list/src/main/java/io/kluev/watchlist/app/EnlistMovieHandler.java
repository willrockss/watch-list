package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RequiredArgsConstructor
public class EnlistMovieHandler {

    private final MovieRepository movieRepository;

    public EnlistMovieResponse handle(EnlistMovieRequest request) {
        val movie = createMovieItemByRequest(request);
        movieRepository.enlist(movie);
        return new EnlistMovieResponse(movie.getFullTitle());
    }

    private MovieItem createMovieItemByRequest(EnlistMovieRequest request) {
        if (isNotBlank(request.foreignTitle())) {
            return MovieItem.create(request.title(), request.foreignTitle(), request.year(), request.externalId());
        }
        return MovieItem.create(request.title(), request.year(), request.externalId());
    }
}
