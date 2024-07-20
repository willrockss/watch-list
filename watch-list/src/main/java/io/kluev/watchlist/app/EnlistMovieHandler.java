package io.kluev.watchlist.app;

import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RequiredArgsConstructor
public class EnlistMovieHandler {

    private final MovieRepository movieRepository;
    private final ApplicationEventPublisher publisher;

    public EnlistMovieResponse handle(EnlistMovieRequest request) {
        val movie = createMovieItemByRequest(request);
        movieRepository.enlist(movie);
        publisher.publishEvent(new MovieEnlisted(movie, request.username()));
        return new EnlistMovieResponse(movie.getFullTitle());
    }

    private MovieItem createMovieItemByRequest(EnlistMovieRequest request) {
        if (isNotBlank(request.foreignTitle())) {
            return MovieItem.create(request.title(), request.foreignTitle(), request.year(), request.externalId());
        }
        return MovieItem.create(request.title(), request.year(), request.externalId());
    }
}
