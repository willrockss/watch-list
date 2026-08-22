package io.kluev.watchlist.infra.temporal;

import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@ConditionalOnProperty(name = "workflow-engine", havingValue = "TEMPORAL")
@Slf4j
@RequiredArgsConstructor
@Component
public class TemporalCallbackHandler {

    private final WorkflowClient workflowClient;

    public static final String TEMPORAL_WORKFLOW_CALLBACK_MARKER = "T";
    public static final String TEMPORAL_ASYNC_ACTIVITY_CALLBACK_MARKER = "A";
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

    @Order(1)
    @EventListener(ChatMessageResponse.class)
    public void listenAsyncActivityCallback(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        if (commandText.startsWith(TEMPORAL_ASYNC_ACTIVITY_CALLBACK_MARKER)) {
            completeActivityAsync(chatResponse);
        } else {
            log.debug("{} is not related to Temporal Async Activity Callback", chatResponse);
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

    private void completeActivityAsync(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        String[] parts = commandText.split(TEMPORAL_CALLBACK_DELIMITER, 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid async activity callback command " + commandText);
        }
        int i = 0;
        String taskTokenBase64 = parts[++i];
        String result = parts[++i];

        byte[] taskToken = Base64.getDecoder().decode(taskTokenBase64);

        ActivityCompletionClient activityCompletionClient = workflowClient.newActivityCompletionClient();
        activityCompletionClient.complete(taskToken, result);
        log.debug("Async activity completed with result {}", result);
    }
}