package io.kluev.watchlist.app.chat;

public record ChatMessage(
        String id,
        String username,
        String chatId,
        String text
) {
}
