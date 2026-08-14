package io.kluev.watchlist.app.searchmovie.temporal;

import io.temporal.activity.ActivityInterface;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistMovieResponse;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.app.EnlistWatchedMovieResponse;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.app.ExternalMovieDatabase;

import java.util.List;

@ActivityInterface
public interface SearchMovieActivities {

    List<ExternalMovieDatabase.ExternalMovieDto> searchInExternalMovieDb(String query);

    void sendChatMessage(ChatGateway.MessageArgs message);

    void sendSimpleChatMessage(String chatId, String messageTemplate, Object... args);

    EnlistMovieResponse addToWatchList(EnlistMovieRequest request);

    EnlistWatchedMovieResponse enlistWatchedMovie(EnlistWatchedMovieRequest request);
}