package io.kluev.watchlist.infra.telegrambot;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramSessionStore {

    void createOrUpdateSession(Update incomingUpdate);
}
