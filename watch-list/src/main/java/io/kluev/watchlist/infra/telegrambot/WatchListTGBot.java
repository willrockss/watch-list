package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsWatchListRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;

@RequiredArgsConstructor
public class WatchListTGBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final String botApiKey;
    private final Set<String> allowedUsers;
    private final TelegramClient telegramClient;
    private final ExternalMovieDatabase externalMovieDatabase;
    private final GoogleSheetsWatchListRepository googleSheetsExample;

    private static final LinkPreviewOptions SMALL_PREVIEW =
            LinkPreviewOptions
                    .builder()
                    .preferSmallMedia(true)
                    .build();

    @SneakyThrows
    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            processMessage(update);
        } else if (update.hasCallbackQuery()) {
            processCallback(update);
        }
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
                var fullTitle = "%s(%d, %s)".formatted(movieDto.name(), movieDto.year(), movieDto.enName());
                googleSheetsExample.addMovieToWatch(fullTitle, "", extId);
                msgBuilder.text("Фильм " + fullTitle + " добавлен в список");
            } else {
                msgBuilder.text("Invalid id " + extId);
            }
        }

        telegramClient.execute(msgBuilder.build());

    }

    @SneakyThrows
    private void processMessage(Update update) {
        var msg = update.getMessage();
        System.out.println(msg.getText());

        if (!allowedUsers.contains(msg.getFrom().getUserName())) {
            return;
        }

        // Ignore commands for now
        if(msg.getText().startsWith("/")) {
            return;
        }

        var chatId = msg.getChatId().toString();

        var foundMovies = externalMovieDatabase.find(msg.getText());
        if (foundMovies.isEmpty()) {
            telegramClient.execute(new SendMessage(chatId, "По запросу '%s' ничего не найдено".formatted(msg.getText())));
        } else {
            foundMovies.stream().findFirst().ifPresent(it -> {
                var fullName = "%s(%d, %s)".formatted(it.name(), it.year(), it.enName());
                SendMessage img1Msg = new SendMessage(chatId, "%s\n%s".formatted(fullName, it.previewImageUrl()));
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
