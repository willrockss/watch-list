package io.kluev.watchlist.infra.vkbot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallback;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallbackType;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import com.vk.api.sdk.queries.messages.MessagesSendQueryWithUserIds;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.infra.chat.ChatSessionStore;
import io.kluev.watchlist.infra.config.props.VkontakteBotProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
public class VkChatGateway implements ChatGateway {

    private final Random rand = new Random(System.currentTimeMillis());
    private final Gson gson = new Gson();
    private final VkApiClient vk;
    private final GroupActor groupActor;
    private final VkontakteBotProperties props;
    private final ChatSessionStore chatSessionStore;

    public VkChatGateway(VkontakteBotProperties props, ChatSessionStore chatSessionStore) {
        this.props = props;
        TransportClient transportClient = new HttpTransportClient();
        vk = new VkApiClient(transportClient);
        groupActor = new GroupActor(props.getGroupId(), props.getGroupToken());
        this.chatSessionStore = chatSessionStore;
    }

    @Override
    public void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found) {
        val chatIds = getAdminsChatsIds();
        Assert.state(!ObjectUtils.isEmpty(chatIds), "No active admins session present!");

        for (String chatId : chatIds) {
            sendSelectContentRequest(chatId, sagaId, found);
        }
    }

    private List<String> getAdminsChatsIds() {
        return chatSessionStore.findChatIdsByRawUserIds(props.getAdmins());
    }

    private void sendSelectContentRequest(String adminChatId, UUID sagaId, List<DownloadableContentInfo> found) {
        sendMessage(MessageArgs.builder()
                .chatId(adminChatId)
                .messageTemplate(generateText(found))
                .buttons(generateKeyboard(sagaId, found))
                .build());
    }

    private List<List<CommandButton>> generateKeyboard(UUID sagaId, List<DownloadableContentInfo> found) {
        val result = new ArrayList<List<CommandButton>>();

        val size = found.size();

        for (int i = 0; i < size; i += 2) {
            var buttonRow = new ArrayList<CommandButton>();
            buttonRow.add(
                    CommandButton
                            .builder()
                            .caption(String.valueOf(i + 1))
                            .action("searchContentSaga_" + sagaId + "_s_" + (i + 1))
                            .build()
            );

            if (i + 1 < size) {
                buttonRow.add(
                        CommandButton
                                .builder()
                                .caption(String.valueOf(i + 2))
                                .action("searchContentSaga_" + sagaId + "_s_" + (i + 2))
                                .build()
                );
            }
            result.add(buttonRow);
        }
        return result;
    }

    private String generateText(List<DownloadableContentInfo> found) {
        val textBuilder = new StringBuilder("Выберите контент для добавления в список: \n\n");
        for (int i = 0; i < found.size(); i++) {
            val elem = found.get(i);
            val readableSize = FileUtils.byteCountToDisplaySize(elem.getSize());
            textBuilder.append("\t%d. %s. \n[Размер: %s, Скачан раз: %d]\n\n".formatted(i + 1, elem.getTitle(), readableSize, elem.getStatistics().getDownloadedTimes()));
        }
        return textBuilder.toString();
    }

    @SneakyThrows
    @Override
    public void sendMessage(MessageArgs args) {
        String message = args.messageTemplate();
        if (!CollectionUtils.isEmpty(args.templateArgs())) {
            message = message.formatted(args.templateArgs().toArray());
        }
        MessagesSendQueryWithUserIds msgBuilder = vk.messages()
                .sendUserIds(groupActor)
                .peerId(Long.valueOf(args.chatId()))
                .randomId(rand.nextInt(10000))
                .message(message)
                .dontParseLinks(false);

        if (!isEmpty(args.buttons())) {
            addKeyboard(msgBuilder, args);
        }

        if (StringUtils.isNumeric(args.replyMessageId())) {
            msgBuilder.replyTo(Integer.valueOf(args.replyMessageId()));
        }
        // Need to parse manually because of outdated SDK
        val response = msgBuilder.executeAsString();
        log.debug("Message send result {}", response);
        val responseBody = gson.fromJson(response, JsonElement.class);
        if (responseBody.isJsonObject()) {
            val error = responseBody.getAsJsonObject().get("error");
            if (error != null && error.isJsonObject()) {
                val errorMsg = error.getAsJsonObject().get("error_msg");
                if (errorMsg != null) {
                    throw new RuntimeException(errorMsg.toString());
                }
            }
        }
    }

    private void addKeyboard(MessagesSendQueryWithUserIds msgBuilder, MessageArgs args) {
        Objects.requireNonNull(args.buttons(), "Not buttons are specified!");
        Assert.notEmpty(args.buttons(), () -> "Not buttons are specified!");

        List<List<KeyboardButton>> buttons = new ArrayList<>();
        for (List<CommandButton> buttonRow : args.buttons()) {
            var buttonsInRow = buttonRow.stream()
                    .filter(it -> it.condition() == null || it.condition().get())
                    .map(it -> new KeyboardButton()
                            .setColor(KeyboardButtonColor.POSITIVE)
                            .setAction(new KeyboardButtonActionCallback()
                                    .setType(KeyboardButtonActionCallbackType.CALLBACK)
                                    .setLabel(it.caption())
                                    .setPayload("{\"cmd\": \"%s\"}".formatted(it.action())))
                    )
                    .toList();
            if (!buttonsInRow.isEmpty()) {
                buttons.add(buttonsInRow);
            }
        }
        if (!buttons.isEmpty()) {
            Keyboard keyboard = new Keyboard()
                    .setInline(true)
                    .setButtons(buttons);
            msgBuilder.keyboard(keyboard);
        }
    }
}
