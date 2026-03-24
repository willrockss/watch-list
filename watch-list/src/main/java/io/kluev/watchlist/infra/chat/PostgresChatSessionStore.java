package io.kluev.watchlist.infra.chat;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Repository
@RequiredArgsConstructor
public class PostgresChatSessionStore implements ChatSessionStore {

    public static final String UPSERT_SESSION_SQL =
            """
            insert into chat_session (source, raw_user_id, chat_id)
            values (:source, :raw_user_id, :chatId)

            on conflict (source, raw_user_id)
            do update set last_message_received_at = now()
            """;

    public static final String SELECT_CHATS_TEMPLATE_SQL =
            """
            select chat_id from chat_session
            where raw_user_id in (:rawUserIds)
            """;

    private final JdbcClient jdbcClient;

    @Override
    @Transactional
    public void createOrUpdateSession(String source, String rawUserId, String chatId) {
        jdbcClient
                .sql(UPSERT_SESSION_SQL)
                .param("source", source)
                .param("raw_user_id", rawUserId)
                .param("chatId", chatId)
                .update();
    }

    @Override
    public List<String> findChatIdsByRawUserIds(@NotNull Collection<String> rawUserIds) {
        if (isEmpty(rawUserIds)) {
            return List.of();
        }

        return jdbcClient
                .sql(SELECT_CHATS_TEMPLATE_SQL)
                .param("rawUserIds", rawUserIds)
                .query((rs, rowNum) -> rs.getString("chat_id"))
                .list();
    }
}
