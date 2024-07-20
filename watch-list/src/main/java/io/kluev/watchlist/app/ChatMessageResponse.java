package io.kluev.watchlist.app;

public record ChatMessageResponse(
        String chatId,
        String responseText
) {
}
