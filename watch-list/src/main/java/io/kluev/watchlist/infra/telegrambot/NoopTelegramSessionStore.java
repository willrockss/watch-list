package io.kluev.watchlist.infra.telegrambot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collection;
import java.util.List;

@Slf4j
public class NoopTelegramSessionStore implements TelegramSessionStore {

    @Override
    public void createOrUpdateSession(Update incomingUpdate) {
        noopDebugLog();
    }

    @Override
    public List<String> findChatIdsByUsernames(Collection<String> admins) {
        noopDebugLog();
        return List.of();
    }

    private void noopDebugLog() {
        log.debug("StubTelegramSessionStore is used. Do nothing");
    }
}
