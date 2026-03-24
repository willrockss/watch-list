package io.kluev.watchlist.infra.chat;

import java.util.Collection;
import java.util.List;

public interface ChatSessionStore {

    void createOrUpdateSession(String source, String rawUserId, String chatId);

    List<String> findChatIdsByRawUserIds(Collection<String> usernames);
}
