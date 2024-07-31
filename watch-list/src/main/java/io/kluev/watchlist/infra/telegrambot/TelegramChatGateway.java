package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@RequiredArgsConstructor
public class TelegramChatGateway implements ChatGateway {
    private final TelegramClient telegramClient;
    private final TelegramSessionStore telegramSessionStore;
    private final TelegramBotProperties telegramBotProperties;

    @Override
    public void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found) {
        val chatIds = getAdminsChatsIds();
        Assert.state(!isEmpty(chatIds), "No active admins session present!");

        for (String chatId : chatIds) {
            sendSelectContentRequest(chatId, sagaId, found);
        }
    }

    private Iterable<String> getAdminsChatsIds() {
        return telegramSessionStore.findChatIdsByUsernames(telegramBotProperties.getAdmins());
    }

    private void sendSelectContentRequest(String adminChatId, UUID sagaId, List<DownloadableContentInfo> found) {
        SendMessage msg = new SendMessage(adminChatId, generateText(found));
        msg.setReplyMarkup(generateKeyboard(sagaId, found));
        msg.setParseMode("MarkdownV2");
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateText(List<DownloadableContentInfo> found) {
        val textBuilder = new StringBuilder("Выберите контент для добавления в список: \n\n");
        for (int i = 0; i < found.size(); i++) {
            val elem = found.get(i);
            val readableSize = FileUtils.byteCountToDisplaySize(elem.size());
            textBuilder.append("\t*%d*. %s. \n\\[Размер: `%s`, Скачан раз: `%d`\\]\n\n".formatted(i + 1, elem.title(), readableSize, elem.statistics().downloadedTimes()));
        }
        return escapeMarkdown(textBuilder.toString());
    }

    private String escapeMarkdown(String text) {
        return text
                .replace(".", "\\.")
                .replace("-", "\\-")
                .replace("+", "\\+")
                .replace(">", "\\>")
                .replace("<", "\\<")
                .replace("|", "\\|")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private ReplyKeyboard generateKeyboard(UUID sagaId, List<DownloadableContentInfo> found) {
        val keyboardBuilder = InlineKeyboardMarkup.builder();

        val size = found.size();

        for (int i = 0; i < size; i += 2) {
            var buttonRow = new ArrayList<InlineKeyboardButton>();
            buttonRow.add(
                    InlineKeyboardButton
                            .builder()
                            .text(String.valueOf(i + 1))
                            .callbackData("searchContentSaga_" + sagaId + "_s_" + (i + 1))
                            .build()
            );

            if (i + 1 < size) {
                buttonRow.add(
                        InlineKeyboardButton
                                .builder()
                                .text(String.valueOf(i + 2))
                                .callbackData("searchContentSaga_" + sagaId + "_s_" + (i + 2))
                                .build()
                );
            }
            keyboardBuilder.keyboardRow(new InlineKeyboardRow(buttonRow));
        }
        return keyboardBuilder.build();
    }

    @Override
    public void sendMessage(String chatId, String msg) {
        SendMessage sm = new SendMessage(chatId, msg);
        try {
            telegramClient.execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
