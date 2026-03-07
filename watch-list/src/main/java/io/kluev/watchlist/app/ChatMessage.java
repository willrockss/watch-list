package io.kluev.watchlist.app;

public record ChatMessage(
        String id,
        String username,
        String chatId,
        String message
) {
}
