package io.kluev.watchlist.app.chat;

public record ChatMessageResponse(
        String messageId,
        String chatId,
        String responseText,
        String username
) {

    public ChatMessageResponse changeResponseText(String newResponseText) {
        return new ChatMessageResponse(this.messageId, this.chatId, newResponseText, this.username);
    }
}
