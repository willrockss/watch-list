package io.kluev.watchlist.infra.telegrambot;

import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// TODO Implement proper TestContainers test
@SpringBootTest
@Disabled
class PgTelegramSessionStorePlaygroundIT {

    @Autowired
    private TelegramSessionStore telegramSessionStore;

    @Test
    public void should_find_admins_chat_ids() {
        val result = telegramSessionStore.findChatIdsByUsernames(Set.of("cranman89"));
        assertThat(result).isNotEmpty();
    }
}