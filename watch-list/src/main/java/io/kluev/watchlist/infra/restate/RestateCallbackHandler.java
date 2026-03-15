package io.kluev.watchlist.infra.restate;

import dev.restate.client.Client;
import dev.restate.common.Request;
import dev.restate.common.Target;
import dev.restate.serde.TypeTag;
import io.kluev.watchlist.app.KeyValueStorage;
import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestateCallbackHandler {

    private final Client restateClient;
    private final KeyValueStorage keyValueStorage;

    public static final String RESTATE_AWAKEABLE_CALLBACK_MARKER = "<-";
    public static final String RESTATE_WORKFLOW_CALLBACK_MARKER = "@";
    public static final String RESTATE_SHORTENED_WORKFLOW_CALLBACK_MARKER = "⤓";
    public static final String RESTATE_CALLBACK_DELIMITER = "\\|";

    // TODO refactor
    @Order(0)
    @EventListener(ChatMessageResponse.class)
    public void listenChangeResponse(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        if (commandText.startsWith(RESTATE_AWAKEABLE_CALLBACK_MARKER)) {
            String[] parts = commandText.split(RESTATE_CALLBACK_DELIMITER, 3);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid callback command " + commandText);
            }
            String callbackId = parts[1];
            String commandData = parts[2];
            restateClient.awakeableHandle(callbackId)
                    .resolve(CallbackCommand.class, new CallbackCommand(chatResponse, commandData));
        } else if (commandText.startsWith(RESTATE_WORKFLOW_CALLBACK_MARKER)) {
            processWorkflowCallback(chatResponse);
        } else if(commandText.startsWith(RESTATE_SHORTENED_WORKFLOW_CALLBACK_MARKER)) {
            processShortenedWorkflowCallback(chatResponse);
        } else {
            log.debug("{} is not related to Restate Callback", chatResponse);
        }
    }

    private void processWorkflowCallback(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        String[] parts = commandText.split(RESTATE_CALLBACK_DELIMITER, 5);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid workflow callback command " + commandText);
        }
        int i = 0;
        String workflowName = parts[++i];
        String workflowKey = parts[++i];
        String callbackMethodName = parts[++i];
        String commandData = parts[++i];

        Target target = Target.workflow(workflowName, workflowKey, callbackMethodName);

        CallbackCommand command = new CallbackCommand(chatResponse, commandData);

        Request<CallbackCommand, Void> req = Request.of(target, TypeTag.of(CallbackCommand.class), TypeTag.of(Void.class), command)
                .idempotencyKey(chatResponse.messageId())
                .build();

        val response = restateClient.call(req);
        System.out.println(response.response());
    }

    private void processShortenedWorkflowCallback(ChatMessageResponse chatResponse) {
        String commandText = defaultIfNull(chatResponse.responseText(), "");
        String[] parts = commandText.split(RESTATE_CALLBACK_DELIMITER, 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid compressed workflow callback command " + commandText);
        }
        String key = parts[1];
        String uncompressedCommand = keyValueStorage.getValue(key)
                .orElseThrow(() -> new IllegalArgumentException("Value is absent for compressed key " + key));
        ChatMessageResponse uncompressedResponse = chatResponse.changeResponseText(uncompressedCommand);
        processWorkflowCallback(uncompressedResponse);
    }
}
