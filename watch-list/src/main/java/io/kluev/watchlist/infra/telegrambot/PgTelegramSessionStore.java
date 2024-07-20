package io.kluev.watchlist.infra.telegrambot;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collection;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@RequiredArgsConstructor
public class PgTelegramSessionStore implements TelegramSessionStore {

    public static final String UPSERT_SESSION_SQL =
            """
            insert into telegram_session (username, chat_id)
            values (?, ?)
            on conflict (username) do update set last_message_received_at = current_timestamp
            """;

    public static final String SELECT_CHATS_TEMPLATE_SQL =
            """
            select chat_id from telegram_session
            where username in (:adminUsernames)
            """;

    private final JdbcClient jdbcClient;

    @Override
    @Transactional
    public void createOrUpdateSession(Update incomingUpdate) {
        val msg = incomingUpdate.getMessage();
        jdbcClient
                .sql(UPSERT_SESSION_SQL)
                .params(msg.getFrom().getUserName(), msg.getChatId())
                .update();
    }

    @Override
    public List<String> findChatIdsByUsernames(@NotNull Collection<String> admins) {
        if (isEmpty(admins)) {
            return List.of();
        }

        val recordsList = jdbcClient
                .sql(SELECT_CHATS_TEMPLATE_SQL)
                .param("adminUsernames", admins)
                .query()
                .listOfRows();
        return recordsList.stream().map(it -> String.valueOf(it.get("chat_id")))
                .toList();
    }
}
