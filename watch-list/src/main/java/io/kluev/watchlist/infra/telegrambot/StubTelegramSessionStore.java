package io.kluev.watchlist.infra.telegrambot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
public class StubTelegramSessionStore implements TelegramSessionStore {

    @Override
    public void createOrUpdateSession(Update incomingUpdate) {
        log.debug("StubTelegramSessionStore is used. Do nothing");
    }
}
