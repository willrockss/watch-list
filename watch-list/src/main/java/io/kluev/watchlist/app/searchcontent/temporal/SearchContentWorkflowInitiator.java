package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.app.common.temporal.TemporalQueues;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "workflow-engine", havingValue = "TEMPORAL")
@RequiredArgsConstructor
public class SearchContentWorkflowInitiator {

    private final WorkflowClient workflowClient;

    // TODO use SearchContentRequest as WF input
    public void start(MovieEnlisted event) {
        val wfId = "search-content-for-ext-" + event.movie().getExternalId();
        val options = WorkflowOptions.newBuilder()
                .setWorkflowId(wfId)
                .setTaskQueue(TemporalQueues.DEFAULT_WORKFLOW_QUEUE)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .build();
        val workflow = workflowClient.newWorkflowStub(SearchContentWorkflow.class, options);

        val movie = event.movie();
        val request = new SearchContentRequest(
                wfId, movie.getTitle(), movie.getForeignTitle(), movie.getYear(), movie.getExternalId());
        try {
            val execution = WorkflowClient.start(workflow::run, request);
            log.info("SearchContentWorkflow(workflowId={}, runId={}) was successfully started.",
                    wfId, execution.getRunId());
        } catch (WorkflowExecutionAlreadyStarted alreadyStarted) {
            val execution = alreadyStarted.getExecution();
            log.info("SearchContentWorkflow(workflowId={}, runId={}) was successfully started earlier.",
                    wfId, execution.getRunId());
        }
    }

    @EventListener(MovieEnlisted.class)
    public void onMovieEnlisted(MovieEnlisted event) {
        start(event);
    }
}