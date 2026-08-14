package io.kluev.watchlist.infra.temporal;

import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@ConditionalOnProperty(name = "workflow-engine", havingValue = "TEMPORAL")
@Slf4j
@RequiredArgsConstructor
@Component
public class TemporalCallbackHandler {

    private final WorkflowClient workflowClient;

    public static final String TEMPORAL_WORKFLOW_CALLBACK_MARKER = "T";
    public static final String TEMPORAL_CALLBACK_DELIMITER = "\\|";

    @Order(0)
    @EventListener(ChatMessageResponse.class)
    public void listenChangeResponse(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        if (commandText.startsWith(TEMPORAL_WORKFLOW_CALLBACK_MARKER)) {
            processWorkflowCallback(chatResponse);
        } else {
            log.debug("{} is not related to Temporal Callback", chatResponse);
        }
    }

    private void processWorkflowCallback(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        String[] parts = commandText.split(TEMPORAL_CALLBACK_DELIMITER, 5);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid workflow callback command " + commandText);
        }
        int i = 1;
        String workflowKey = parts[++i];
        String callbackMethodName = parts[++i];
        String commandData = parts[++i];

        CallbackCommand command = new CallbackCommand(chatResponse, commandData);

        WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowKey);
        workflowStub.signal(callbackMethodName, command);
        log.debug("Signal {} sent to workflow {}", callbackMethodName, workflowKey);
    }
}