package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.ChatMessage;
import io.kluev.watchlist.app.ChatMessageResponse;
import io.kluev.watchlist.app.EnlistMovieHandler;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistWatchedMovieHandler;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class WatchListTGBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final String botApiKey;
    private final Set<String> allowedUsers;
    private final TelegramClient telegramClient;
    private final ExternalMovieDatabase externalMovieDatabase;
    private final EnlistMovieHandler enlistMovieHandler;
    private final EnlistWatchedMovieHandler enlistWatchedMovieHandler;
    private final TelegramSessionStore telegramSessionStore;
    private final ApplicationEventPublisher eventPublisher;

    private static final LinkPreviewOptions SMALL_PREVIEW =
            LinkPreviewOptions
                    .builder()
                    .preferSmallMedia(true)
                    .build();

    @SneakyThrows
    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            processCallback(update);
        }

        if (!update.hasMessage()) {
            log.debug("Message {} not a text message. Skip", update);
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
        Integer messageId = initialMessage.getMessageId();
        String chatId = String.valueOf(initialMessage.getChatId());

        answerCallbackQuery(update.getCallbackQuery().getId());

        eventPublisher.publishEvent(new ChatMessageResponse(
                chatId,
                callData
        ));

        // Move to Search saga
        if (callData.startsWith("add_movie_watched_")) {
            var msgBuilder = SendMessage
                    .builder()
                    .chatId(chatId)
                    .replyToMessageId(messageId);


            var extId = callData.replace("add_movie_watched_", "");
            var movieDto = externalMovieDatabase.getByExternalId(extId).orElse(null);
            if (movieDto != null) {

                var watchAtInstant = Instant.ofEpochSecond(update.getCallbackQuery().getMessage().getDate());
                // TODO take timezone from User configuration
                var watchAt = watchAtInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                var enlistRequest = EnlistWatchedMovieRequest
                        .builder()
                        .title(movieDto.name())
                        .foreignTitle(movieDto.enName())
                        .year(movieDto.year())
                        .externalId(extId)
                        .watchedAt(watchAt)
                        .username(update.getCallbackQuery().getFrom().getUserName())
                        .build();

                var response = enlistWatchedMovieHandler.handle(enlistRequest);

                msgBuilder.text("Фильм " + response.fullTitle() + " добавлен как просмотренный");
            } else {
                msgBuilder.text("Invalid id " + extId);
            }

            telegramClient.execute(msgBuilder.build());
        } else if (callData.startsWith("add_movie_")) {
            var msgBuilder = SendMessage
                    .builder()
                    .chatId(chatId)
                    .replyToMessageId(messageId);


            var extId = callData.replace("add_movie_", "");
            var movieDto = externalMovieDatabase.getByExternalId(extId).orElse(null);
            if (movieDto != null) {
                var enlistRequest = EnlistMovieRequest
                        .builder()
                        .title(movieDto.name())
                        .foreignTitle(movieDto.enName())
                        .year(movieDto.year())
                        .externalId(extId)
                        .username(update.getCallbackQuery().getFrom().getUserName())
                        .build();

                var response = enlistMovieHandler.handle(enlistRequest);

                msgBuilder.text("Фильм " + response.fullTitle() + " добавлен в список");
            } else {
                msgBuilder.text("Invalid id " + extId);
            }

            telegramClient.execute(msgBuilder.build());
        }
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

        eventPublisher.publishEvent(new ChatMessage(msg.getFrom().getUserName(), chatId, msg.getText()));

        var foundMovies = externalMovieDatabase.find(msg.getText());

        SendMessage respMsg;
        if (foundMovies.isEmpty()) {
            respMsg = new SendMessage(chatId, "По запросу '%s' ничего не найдено".formatted(msg.getText()));
        } else {
            val firstFoundMovie = foundMovies.stream().findFirst().orElseThrow();
            respMsg = new SendMessage(chatId, "%s\n%s".formatted(firstFoundMovie.getFullName(), firstFoundMovie.previewImageUrl()));
            respMsg.setLinkPreviewOptions(SMALL_PREVIEW);
            respMsg.setReplyMarkup(
                    InlineKeyboardMarkup
                            .builder()
                            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton
                                    .builder()
                                    .text("Добавить в список")
                                    .callbackData("add_movie_" + firstFoundMovie.externalId())
                                    .build())
                            )
                            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton
                                    .builder()
                                    .text("Добавить просмотренным")
                                    .callbackData("add_movie_watched_" + firstFoundMovie.externalId())
                                    .build())
                            )
                            .build()
            );
        }

        telegramClient.execute(respMsg);
    }

    private void answerCallbackQuery(String callbackQueryId) {
        // vvv
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
