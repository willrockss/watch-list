package io.kluev.watchlist.app.searchmovie.temporal;

import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistMovieResponse;
import io.kluev.watchlist.app.EnlistWatchedMovieHandler;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.app.EnlistWatchedMovieResponse;
import io.kluev.watchlist.app.ExternalMovieDatabase;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.app.searchcontent.temporal.SearchContentWorkflowInitiator;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

// TODO Split into separate activities
@ActivityImpl(taskQueues = "watch-list-activity-worker")
@ConditionalOnProperty(name = "workflow-engine", havingValue = "TEMPORAL")
@Component("searchMovieActivitiesBean")
@RequiredArgsConstructor
public class SearchMovieActivitiesImpl implements SearchMovieActivities {

    private final ExternalMovieDatabase externalMovieDatabase;
    private final ChatGateway chatGateway;
    private final MovieRepository movieRepository;
    private final SearchContentWorkflowInitiator searchContentWorkflowInitiator;
    private final EnlistWatchedMovieHandler enlistWatchedMovieHandler;

    @Override
    public List<ExternalMovieDatabase.ExternalMovieDto> searchInExternalMovieDb(String query) {
        return externalMovieDatabase.find(query);
    }

    @Override
    public void sendChatMessage(ChatGateway.MessageArgs message) {
        chatGateway.sendMessage(message);
    }

    @Override
    public void sendSimpleChatMessage(String chatId, String messageTemplate, Object... args) {
        val stringArgs = Arrays.stream(args).map(String::valueOf).toArray(String[]::new);
        chatGateway.sendMessage(chatId, messageTemplate, stringArgs);
    }

    @Override
    public EnlistMovieResponse addToWatchList(EnlistMovieRequest request) {
        val movie = createMovieItemByRequest(request);
        movieRepository.enlist(movie);
        return new EnlistMovieResponse(movie);
    }

    @Override
    public void startContentSearch(MovieItem movie, EnlistMovieRequest request) {
        searchContentWorkflowInitiator.start(new MovieEnlisted(movie, request.username()));
    }

    @Override
    public EnlistWatchedMovieResponse enlistWatchedMovie(EnlistWatchedMovieRequest request) {
        return enlistWatchedMovieHandler.handle(request);
    }

    private MovieItem createMovieItemByRequest(EnlistMovieRequest request) {
        if (isNotBlank(request.foreignTitle())) {
            return MovieItem.create(request.title(), request.foreignTitle(), request.year(), request.externalId());
        }
        return MovieItem.create(request.title(), request.year(), request.externalId());
    }
}