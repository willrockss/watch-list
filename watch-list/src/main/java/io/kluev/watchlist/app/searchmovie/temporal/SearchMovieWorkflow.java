package io.kluev.watchlist.app.searchmovie.temporal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.searchmovie.SearchMovieRequest;

@WorkflowInterface
public interface SearchMovieWorkflow {

    @WorkflowMethod
    void run(SearchMovieRequest req);

    @SuppressWarnings("unused")
    @SignalMethod
    void processUserCommand(CallbackCommand command);
}