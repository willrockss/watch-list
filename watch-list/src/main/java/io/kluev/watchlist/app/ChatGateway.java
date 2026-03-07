package io.kluev.watchlist.app;

import lombok.Builder;
import lombok.NonNull;

import java.util.List;
import java.util.UUID;

public interface ChatGateway {
    void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found);

    void sendMessage(String chatId, String notificationTemplate, String... args);

    void sendMessage(MessageArgs args);

    @Builder
    record MessageArgs(
            @NonNull String chatId,
            @NonNull String messageTemplate,
            @NonNull List<String> templateArgs,
            @NonNull List<List<CommandButton>> buttons
    ) {}

    @Builder
    record CommandButton(
            @NonNull String caption,
            @NonNull String action
    ) {}
}
