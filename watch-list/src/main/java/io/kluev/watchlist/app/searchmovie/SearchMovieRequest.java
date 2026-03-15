package io.kluev.watchlist.app.searchmovie;

public record SearchMovieRequest(
        String chatId,
        String messageId,
        String query
) {
}
