package io.kluev.watchlist.app.searchmovie;

import dev.restate.client.Client;
import io.kluev.watchlist.app.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "workflow-engine", havingValue = "RESTATE")
public class SearchMovieWorkflowInitiator {

    private final Client restateClient;

    @Order(Integer.MAX_VALUE - 1000)
    @EventListener(ChatMessage.class)
    public void listenUnprocessedChatMessage(ChatMessage msg) {
        var chatId = msg.chatId();
        var msgId = msg.id();

        var wfId = "%s-%s-%s".formatted(msg.source(), chatId, msgId);
        var wfClient = SearchMovieWorkflowClient.fromClient(restateClient, wfId);

        var searchRequest = new SearchMovieRequest(chatId, msgId, msg.text());
        var invId = wfClient.submit(searchRequest).invocationId();
        log.info("SearchWorkflow(invocationId={}) was successfully started.", invId);
    }
}
