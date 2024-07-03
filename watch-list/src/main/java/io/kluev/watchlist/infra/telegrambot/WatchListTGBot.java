package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.EnlistMovieHandler;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class WatchListTGBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final String botApiKey;
    private final Set<String> allowedUsers;
    private final TelegramClient telegramClient;
    private final ExternalMovieDatabase externalMovieDatabase;
    private final EnlistMovieHandler enlistMovieHandler;
    private final TelegramSessionStore telegramSessionStore;

    private static final LinkPreviewOptions SMALL_PREVIEW =
            LinkPreviewOptions
                    .builder()
                    .preferSmallMedia(true)
                    .build();

    @SneakyThrows
    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        if (!isSenderAllowed(update.getMessage())) {
            return;
        }

        telegramSessionStore.createOrUpdateSession(update);

        if (update.hasCallbackQuery()) {
            processCallback(update);
        } else if (update.getMessage().hasText()) {
            processMessage(update);
        } else {
            log.warn("Message {} neither callback response nor text message. Do nothing", update);
        }
    }

    private boolean isSenderAllowed(@NonNull Message message) {
        if (message.getFrom() != null && StringUtils.isNotBlank(message.getFrom().getUserName())) {
            return allowedUsers.contains(message.getFrom().getUserName());
        }
        return false;
    }

    @SneakyThrows
    private void processCallback(Update update) {
        String callData = update.getCallbackQuery().getData();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String chatId = String.valueOf(update.getCallbackQuery().getMessage().getChatId());

        var msgBuilder = SendMessage
                .builder()
                .chatId(chatId)
                .replyToMessageId(messageId);

        if (callData.startsWith("add_movie_")) {
            var extId = callData.replace("add_movie_", "");
            var movieDto = externalMovieDatabase.getByExternalId(extId).orElse(null);
            if (movieDto != null) {
                var enlistRequest = EnlistMovieRequest
                        .builder()
                        .title(movieDto.name())
                        .foreignTitle(movieDto.enName())
                        .year(movieDto.year())
                        .externalId(extId)
                        .build();

                var response = enlistMovieHandler.handle(enlistRequest);

                msgBuilder.text("Фильм " + response.fullTitle() + " добавлен в список");
            } else {
                msgBuilder.text("Invalid id " + extId);
            }
        }

        telegramClient.execute(msgBuilder.build());

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

        var foundMovies = externalMovieDatabase.find(msg.getText());
        if (foundMovies.isEmpty()) {
            telegramClient.execute(new SendMessage(chatId, "По запросу '%s' ничего не найдено".formatted(msg.getText())));
        } else {
            foundMovies.stream().findFirst().ifPresent(it -> {
                SendMessage img1Msg = new SendMessage(chatId, "%s\n%s".formatted(it.getFullName(), it.previewImageUrl()));
                img1Msg.setLinkPreviewOptions(SMALL_PREVIEW);
                img1Msg.setReplyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton
                                        .builder()
                                        .text("Добавить в список")
                                        .callbackData("add_movie_" + it.externalId())
                                        .build())
                                )
                                .build()
                );
                try {
                    telegramClient.execute(img1Msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            });
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
