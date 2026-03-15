package io.kluev.watchlist.app.chat;

import io.kluev.watchlist.app.DownloadableContentInfo;
import lombok.Builder;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public interface ChatGateway {
    void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found);

    void sendMessage(String chatId, String notificationTemplate, String... args);

    void sendMessage(MessageArgs args);

    @Builder
    record MessageArgs(
            @NonNull String chatId,
            @NonNull String messageTemplate,
            @Nullable List<String> templateArgs,
            @Nullable List<List<CommandButton>> buttons,
            @Nullable String replyMessageId
    ) {}

    @Builder
    record CommandButton(
            @NonNull String caption,
            @NonNull String action,
            @Nullable Supplier<Boolean> condition
    ) {}
}
