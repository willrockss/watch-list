package io.kluev.watchlist.infra.telegrambot;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

@RequiredArgsConstructor
public class TelegramSessionStore {

    public static final String UPSERT_SESSION_SQL =
            """
            insert into telegram_session (username, chat_id)
            values (?, ?)
            on conflict (username) do update set last_message_received_at = current_timestamp
            """;

    private final JdbcClient jdbcClient;

    @Transactional
    public void createOrUpdateSession(Update incomingUpdate) {
        val msg = incomingUpdate.getMessage();
        jdbcClient
                .sql(UPSERT_SESSION_SQL)
                .params(msg.getFrom().getUserName(), msg.getChatId())
                .update();
    }
}
