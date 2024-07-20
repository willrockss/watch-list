package io.kluev.watchlist.infra.telegrambot;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collection;
import java.util.List;

public interface TelegramSessionStore {

    void createOrUpdateSession(Update incomingUpdate);

    List<String> findChatIdsByUsernames(Collection<String> admins);
}
