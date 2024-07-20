package io.kluev.watchlist.app;

public record ChatMessage(
        String username,
        String chatId,
        String message
) {
}
