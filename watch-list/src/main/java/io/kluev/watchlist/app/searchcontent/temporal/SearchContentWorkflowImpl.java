package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.common.temporal.TemporalQueues;
import io.kluev.watchlist.common.utils.NumberUtils;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("unused")
@Slf4j
@WorkflowImpl(taskQueues = TemporalQueues.DEFAULT_WORKFLOW_QUEUE)
public class SearchContentWorkflowImpl implements SearchContentWorkflow {

    private final SearchContentActivities activities = Workflow.newActivityStub(
            SearchContentActivities.class,
            ActivityOptions.newBuilder()
                    .setTaskQueue(TemporalQueues.DEFAULT_ACTIVITY_QUEUE)
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumAttempts(3)
                                    .build())
                    .build());

    private List<DownloadableContentInfo> found;
    private Integer selectedIndex;
    private CallbackCommand command;

    @Override
    public void run(SearchContentRequest req) {
        val movie = req.toMovieItem();
        found = activities.findDownloadableContent(movie);

        val buttonPrefix = "T|%s|%s|processUserCommand|s_".formatted(
                Workflow.getInfo().getWorkflowType(),
                Workflow.getInfo().getWorkflowId());
        activities.sendSelectContentRequest(buttonPrefix, found);

        Workflow.await(() -> selectedIndex != null);

        val content = found.get(selectedIndex);
        val savedFilename = activities.downloadAndSave(content, req.toMovieItem());
        activities.sendMessage(
                command.response().chatId(),
                "`file://%s` был успешно скачан",
                savedFilename.replace("torrent", "tt"));
        activities.publishContentSelected(req.toMovieItem(), savedFilename);
    }

    @Override
    public void processUserCommand(CallbackCommand command) {
        String cmd = command.command();
        String indexRaw = cmd.startsWith("s_") ? cmd.substring(2) : cmd;
        Integer parsed = NumberUtils.parseOrNull(indexRaw);
        int selectedIndex = parsed == null ? -1 : parsed - 1;
        if (selectedIndex < 0 || selectedIndex >= found.size()) {
            log.error("Invalid selected index {}. Found count {}. Ignore", selectedIndex, found.size());
            return;
        }
        this.command = command;
        this.selectedIndex = selectedIndex;
    }
}