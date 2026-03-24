package io.kluev.watchlist.app.chat;

import lombok.Builder;

@Builder
public record ChatMessage(
        String source, // VK, Telegram, etc
        String id,
        String username,
        String chatId,
        String text
) {
}
