package io.kluev.watchlist.app.searchmovie.temporal;

import io.kluev.watchlist.app.chat.ChatMessage;
import io.kluev.watchlist.app.common.temporal.TemporalQueues;
import io.kluev.watchlist.app.searchmovie.SearchMovieRequest;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "workflow-engine", havingValue = "TEMPORAL")
@RequiredArgsConstructor
public class SearchMovieWorkflowInitiator {

    private final WorkflowClient workflowClient;

    @Order(Integer.MAX_VALUE - 1000)
    @EventListener(ChatMessage.class)
    public void listenUnprocessedChatMessage(ChatMessage msg) {
        var chatId = msg.chatId();
        var msgId = msg.id();

        var wfId = "%s-%s-%s".formatted(msg.source(), chatId, msgId);
        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(wfId)
                .setTaskQueue(TemporalQueues.DEFAULT_WORKFLOW_QUEUE)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .build();
        var workflow = workflowClient.newWorkflowStub(SearchMovieWorkflow.class, options);

        var searchRequest = new SearchMovieRequest(chatId, msgId, msg.text());
        try {
            var execution = WorkflowClient.start(workflow::run, searchRequest);
            log.info("SearchWorkflow(workflowId={}, runId={}) was successfully started.",
                    wfId, execution.getRunId());
        } catch (WorkflowExecutionAlreadyStarted alreadyStarted) {
            var execution = alreadyStarted.getExecution();
            log.info("SearchWorkflow(workflowId={}, runId={}) was successfully started earlier.",
                    wfId, execution.getRunId());
        }
    }

}