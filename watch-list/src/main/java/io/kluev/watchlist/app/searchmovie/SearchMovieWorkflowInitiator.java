package io.kluev.watchlist.app.searchmovie;

import dev.restate.client.Client;
import io.kluev.watchlist.app.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SearchMovieWorkflowInitiator {

    private final Client restateClient;

    @Order(Integer.MAX_VALUE - 1000)
    @EventListener(ChatMessage.class)
    public void listenUnprocessedChatMessage(ChatMessage msg) {
        val chatId = msg.chatId();
        val msgId = msg.id();

        val wfId = "TG-%s-%s".formatted(chatId, msgId);
        val wfClient = SearchMovieWorkflowClient.fromClient(restateClient, wfId);

        val searchRequest = new SearchMovieRequest(chatId, msgId, msg.text());
        val invId = wfClient.submit(searchRequest).invocationId();
        log.atInfo().addKeyValue("invocationId", invId).log("SearchWorkflow was successfully started.");
    }
}
