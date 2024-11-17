package io.kluev.watchlist.app;

import java.util.List;
import java.util.UUID;

public interface ChatGateway {
    void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found);

    void sendMessage(String chatId, String notificationTemplate, String... args);
}
