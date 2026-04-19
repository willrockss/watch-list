package io.kluev.watchlist.app.addmovie;

import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.springboot.RestateVirtualObject;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistMovieResponse;
import io.kluev.watchlist.app.searchcontent.SearchContentHandler;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import lombok.RequiredArgsConstructor;
import lombok.val;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RequiredArgsConstructor
@RestateVirtualObject
public class EnlistMovieVirtualObject {

    private final MovieRepository movieRepository;
    private final SearchContentHandler searchContentHandler;

    @Handler
    public EnlistMovieResponse addToWatchList(ObjectContext ctx, EnlistMovieRequest req) {
        val movie = createMovieItemByRequest(req);
        ctx.run(() -> {
            // TODO check if already in the to-watch-list
            movieRepository.enlist(movie);
        });
        // TODO move to another workflow
        ctx.run(() -> searchContentHandler.handle(new MovieEnlisted(movie, req.username())));
        return new EnlistMovieResponse(movie.getFullTitle());
    }

    private MovieItem createMovieItemByRequest(EnlistMovieRequest request) {
        if (isNotBlank(request.foreignTitle())) {
            return MovieItem.create(request.title(), request.foreignTitle(), request.year(), request.externalId());
        }
        return MovieItem.create(request.title(), request.year(), request.externalId());
    }
}
