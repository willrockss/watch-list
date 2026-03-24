package io.kluev.watchlist.infra.vkbot;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.events.CallbackEvent;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.longpoll.responses.GetLongPollEventsResponse;
import com.vk.api.sdk.objects.groups.responses.GetLongPollServerResponse;
import com.vk.api.sdk.objects.users.Fields;
import io.kluev.watchlist.app.chat.ChatMessage;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.kluev.watchlist.infra.chat.ChatSessionStore;
import io.kluev.watchlist.infra.chat.Sources;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;


@Slf4j
public class WatchListVkBot {

    private final VkApiClient vk;
    private final GroupActor groupActor;
    private final ChatSessionStore chatSessionStore;
    private final ApplicationEventPublisher eventPublisher;

    public WatchListVkBot(Long groupId, String groupToken, ChatSessionStore chatSessionStore, ApplicationEventPublisher eventPublisher) {
        TransportClient transportClient = new HttpTransportClient();
        vk = new VkApiClient(transportClient);
        groupActor = new GroupActor(groupId, groupToken);
        this.chatSessionStore = chatSessionStore;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        Thread.startVirtualThread(this::run);
    }

    // TODO Remove
    @SneakyThrows
    public static void main(String[] args) {
        Long groupId = 203728225L;
        String groupKey = "f1fc6ae81a88cab8c23bddf68e1c68e94920d91e3a983c78d98d43f2c92ec3a024f0d4534d49740f7bba4"; // Group Token
        WatchListVkBot bot = new WatchListVkBot(groupId, groupKey, null, (o) -> {});
        bot.run();
    }

    // TODO refactor
    @SneakyThrows
    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                GetLongPollServerResponse serverResponse = vk.groups().getLongPollServer(groupActor, groupActor.getGroupId()).execute();
                String ts = serverResponse.getTs();
                log.debug("initial ts = {}", ts);

                while (!Thread.currentThread().isInterrupted()) {
                    GetLongPollEventsResponse eventsResponse = vk.longPoll()
                            .getEvents(serverResponse.getServer(), serverResponse.getKey(), ts)
                            .waitTime(25)
                            .execute();

                    for (var rawEvent : eventsResponse.getUpdates()) {
                        CallbackEvent event = MessageEvent.parse(rawEvent)
                                .map(it -> (CallbackEvent) it)
                                .orElseGet(() -> CallbackEvent.GSON.fromJson(rawEvent, CallbackEvent.class));

                        switch (event) {
                            case MessageNew messageNew -> {
                                var msg = messageNew.getObject().getMessage();
                                var text = msg.getText();
                                Long userId = msg.getFromId();

                                chatSessionStore.createOrUpdateSession(Sources.VK_SOURCE, String.valueOf(userId), String.valueOf(userId));

                                // We don't need to check user is allowed or not because only users from group can write
                                // to bot
                                // TODO Peek login from Session
                                var resp = vk.users().get(groupActor).userIds(userId.toString())
                                        .fields(Fields.NICKNAME, Fields.SCREEN_NAME)
                                        .execute();
                                String login = resp.getFirst().getScreenName();
                                log.debug("New message from {}: {}", login, text);

                                eventPublisher.publishEvent(
                                        ChatMessage.builder()
                                                .source(Sources.VK_SOURCE)
                                                .id(String.valueOf(messageNew.getObject().getMessage().getId()))
                                                .username(login)
                                                .chatId(userId.toString())
                                                .text(msg.getText())
                                                .build()
                                );
                            }
                            case MessageEvent callbackAction -> {
                                log.debug("Got button callback {}", callbackAction);
                                // TODO Peek login from Session
                                var userId = callbackAction.getUserId();
                                var resp = vk.users()
                                        .get(groupActor)
                                        .userIds(String.valueOf(userId))
                                        .fields(Fields.NICKNAME, Fields.SCREEN_NAME)
                                        .execute();
                                String login = defaultIfNull(resp.getFirst().getScreenName(), resp.getFirst().getContactName());

                                eventPublisher.publishEvent(new ChatMessageResponse(
                                        callbackAction.getEventId(),
                                        String.valueOf(callbackAction.getPeerId()),
                                        callbackAction.getPreparedCallbackData(),
                                        login
                                ));
                                vk.messages().sendMessageEventAnswer(groupActor,
                                                callbackAction.getEventId(),
                                                callbackAction.getUserId(),
                                                callbackAction.getPeerId())
                                        .execute();

                            }
                            default -> log.debug("Ignore message {}", event);
                        }
                    }

                    ts = eventsResponse.getTs();
                }
            } catch (Exception e) {
                log.error("Got unexpected {}. Restart VK polling", e.getMessage(), e);
                Thread.sleep(1000);
            }
        }
    }
}
