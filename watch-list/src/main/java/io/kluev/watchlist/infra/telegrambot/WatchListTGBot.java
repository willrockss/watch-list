package io.kluev.watchlist.infra.telegrambot;

import dev.restate.client.Client;
import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.ChatMessage;
import io.kluev.watchlist.app.ChatMessageResponse;
import io.kluev.watchlist.app.EnlistMovieHandler;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistWatchedMovieHandler;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.app.searchmovie.SearchMovieRequest;
import io.kluev.watchlist.app.searchmovie.SearchMovieWorkflowClient;
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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
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
    private final Client restateClient;
    private final ChatGateway chatGateway;

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

        // TODO Move to SearchMovieWorkflow
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
                // TODO use LocalDate.not() instead
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

        eventPublisher.publishEvent(new ChatMessage(msg.getMessageId().toString(), msg.getFrom().getUserName(), chatId, msg.getText()));

        var wfClient = SearchMovieWorkflowClient.fromClient(restateClient, "TG-%s-%s".formatted(chatId, msg.getMessageId()));

        // TODO Move to SearchMovieWorkflow
        var foundMovies = wfClient.submit(new SearchMovieRequest(msg.getText()))
                .attach()
                .response()
                .foundMovies();

        if (foundMovies.isEmpty()) {
            chatGateway.sendMessage(chatId, "По запросу '%s' ничего не найдено", msg.getText());
        } else {
            val firstFoundMovie = foundMovies.stream().findFirst().orElseThrow();
            chatGateway.sendMessage(ChatGateway.MessageArgs.builder()
                            .chatId(chatId)
                            .messageTemplate("%s\n%s")
                            .templateArgs(List.of(firstFoundMovie.getFullName(), firstFoundMovie.previewImageUrl()))
                            .buttons(List.of(
                                    List.of(ChatGateway.CommandButton.builder()
                                            .caption("Добавить в список")
                                            .action("add_movie_" + firstFoundMovie.externalId())
                                            .build()
                                    ),
                                    List.of(ChatGateway.CommandButton.builder()
                                            .caption("Добавить просмотренным")
                                            .action("add_movie_watched_" + firstFoundMovie.externalId())
                                            .build()
                            )))
                    .build());
        }
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
