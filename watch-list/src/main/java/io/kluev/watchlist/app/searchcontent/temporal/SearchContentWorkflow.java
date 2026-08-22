package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.app.chat.CallbackCommand;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SearchContentWorkflow {

    @WorkflowMethod
    void run(SearchContentRequest req);

    @SuppressWarnings("unused")
    @SignalMethod
    void processUserCommand(CallbackCommand command);
}