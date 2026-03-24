package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.chat.ChatMessage;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.kluev.watchlist.infra.chat.Sources;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class WatchListTGBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botApiKey;
    private final Set<String> allowedUsers;
    private final TelegramClient telegramClient;
    private final TelegramSessionStore telegramSessionStore;
    private final ApplicationEventPublisher eventPublisher;

    @SneakyThrows
    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            processCallback(update);
            return;
        }

        if (!update.hasMessage()) {
            log.warn("Message {} not a text. Skip", update);
            return;
        }

        if (!isSenderAllowed(update.getMessage())) {
            return;
        }

        if (!update.getMessage().hasText()) {
            log.warn("Message {} text is absent. Do nothing", update);
        }

        telegramSessionStore.createOrUpdateSession(update);

        processMessage(update);
    }

    private boolean isSenderAllowed(@NonNull Message message) {
        if (message.getFrom() != null && StringUtils.isNotBlank(message.getFrom().getUserName())) {
            val username = message.getFrom().getUserName();
            val isAllowed = allowedUsers.contains(username);
            if (!isAllowed) {
                log.debug("User {} is not allowed. Ignore", username);
            }
            return isAllowed;
        }
        return false;
    }

    @SneakyThrows
    private void processCallback(Update update) {
        String callData = update.getCallbackQuery().getData();
        val initialMessage = update.getCallbackQuery().getMessage();
        val username = update.getCallbackQuery().getFrom().getUserName();
        String chatId = String.valueOf(initialMessage.getChatId());

        answerCallbackQuery(update.getCallbackQuery().getId());

        eventPublisher.publishEvent(new ChatMessageResponse(
                update.getUpdateId().toString(),
                chatId,
                callData,
                username
        ));
    }

    @SneakyThrows
    private void processMessage(Update update) {
        var msg = update.getMessage();
        log.info("Going to process {}", msg.getText());

        if(msg.getText().startsWith("/")) {
            // TODO clear previous session
            return;
        }

        var chatId = msg.getChatId().toString();

        eventPublisher.publishEvent(
                ChatMessage.builder()
                        .source(Sources.TG_SOURCE)
                        .id(msg.getMessageId().toString())
                        .username(msg.getFrom().getUserName())
                        .chatId(chatId)
                        .text(msg.getText())
                        .build()
        );
    }

    private void answerCallbackQuery(String callbackQueryId) {
        val answerCallbackQuery = AnswerCallbackQuery
                .builder()
                .callbackQueryId(callbackQueryId)
                .text("Обрабатывается...")
                .build();

        // Send the response
        try {
            telegramClient.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error("Unable to send AnswerCallBack for callbackQueryId {} due to {}", callbackQueryId, e.toString());
        }
    }

    @Override
    public String getBotToken() {
        return botApiKey;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }
}
