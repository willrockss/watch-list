package io.kluev.watchlist.app.searchmovie.temporal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistMovieResponse;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.app.ExternalMovieDatabase;
import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.kluev.watchlist.app.common.temporal.TemporalQueues;
import io.kluev.watchlist.app.searchmovie.SearchMovieRequest;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Slf4j
@WorkflowImpl(taskQueues = TemporalQueues.DEFAULT_WORKFLOW_QUEUE)
public class SearchMovieWorkflowImpl implements SearchMovieWorkflow {

    private final SearchMovieActivities activities = Workflow.newActivityStub(
            SearchMovieActivities.class,
            ActivityOptions.newBuilder()
                    .setTaskQueue(TemporalQueues.DEFAULT_ACTIVITY_QUEUE)
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumAttempts(3)
                                    .build())
                    .build());

    /** Replay-safe workflow state (equivalent to Restate’s StateKey) */
    private int currentIndex = 0;
    private int callbackCounter = 0;
    private final List<CallbackCommand> pendingCommands = new ArrayList<>();

    @Override
    public void run(SearchMovieRequest req) {
        List<ExternalMovieDatabase.ExternalMovieDto> foundMovies =
                activities.searchInExternalMovieDb(req.query());

        if (foundMovies.isEmpty()) {
            activities.sendSimpleChatMessage(
                    req.chatId(), "По запросу '%s' ничего не найдено", req.query());
            return;
        }

        while (true) {
            int index = currentIndex;

            if (index < 0 || index >= foundMovies.size()) {
                throw ApplicationFailure.newNonRetryableFailure(
                        "Invalid index", "InvalidIndex");
            }

            val currentMovie = foundMovies.get(index);
            val searchResultMessage = ChatGateway.MessageArgs.builder()
                    .chatId(req.chatId())
                    .messageTemplate("%s")
                    .templateArgs(List.of(currentMovie.getFullName()))
                    .image(URI.create(currentMovie.previewImageUrl()))
                    .buttons(List.of(
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Добавить в список")
                                    .action(getShortenedLink("add_" + index))
                                    .build()),
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Добавить просмотренным")
                                    .action(getShortenedLink("watched_" + index))
                                    .build()),
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Ещё (%s/%s)".formatted((index + 2), foundMovies.size()))
                                    .action(getShortenedLink("next_" + (index + 1)))
                                    .condition(index < foundMovies.size() - 1)
                                    .build())
                    ))
                    .build();

            activities.sendChatMessage(searchResultMessage);

            // ---- Wait for the signal that matches the current counter ----
            int expectedCounter = callbackCounter;
            Workflow.await(() -> pendingCommands.size() > expectedCounter);

            CallbackCommand callbackCommand = pendingCommands.get(expectedCounter);
            callbackCounter++; // consume this command
            log.debug("Going to process callback command {}", callbackCommand.command());

            String[] parts = callbackCommand.command().split("_", 2);
            String action = parts[0];
            int actionMovieIndex = Integer.parseInt(parts[1]);

            switch (action) {
                case "next" -> {
                    if (index >= actionMovieIndex) {
                        log.warn("Invalid next command. Going to put next index to {}, but current is {}",
                                actionMovieIndex, index);
                        // counter already consumed above; index stays unchanged
                    } else {
                        currentIndex = actionMovieIndex;
                    }
                }
                case "add" -> {
                    addToWatchList(req.messageId(), foundMovies.get(actionMovieIndex), callbackCommand.response());
                    return;
                }
                case "watched" -> {
                    addAsWatched(req.messageId(), foundMovies.get(actionMovieIndex), callbackCommand.response());
                    return;
                }
                default -> log.error("Unknown action {}. Try again", action);
            }
        }
    }

    @Override
    public void processUserCommand(CallbackCommand command) {
        pendingCommands.add(command);
    }

    private String getShortenedLink(String command) {
        return "T|%s|%s|processUserCommand|%s".formatted(
                Workflow.getInfo().getWorkflowType(),
                Workflow.getInfo().getWorkflowId(),
                command);
    }

    private void addToWatchList(String initialMessageId,
                                ExternalMovieDatabase.ExternalMovieDto movieDto,
                                ChatMessageResponse responseMsg) {
        val enlistRequest = EnlistMovieRequest.builder()
                .title(movieDto.name())
                .foreignTitle(movieDto.enName())
                .year(movieDto.year())
                .externalId(movieDto.externalId())
                .username(responseMsg.username())
                .build();

        EnlistMovieResponse resp = activities.addToWatchList(enlistRequest);

        if (resp.justAdded()) {
            activities.sendChatMessage(ChatGateway.MessageArgs.builder()
                    .chatId(responseMsg.chatId())
                    .replyMessageId(initialMessageId)
                    .messageTemplate("Фильм %s добавлен в список")
                    .templateArgs(List.of(resp.movieItem().getFullTitle()))
                    .build());
        } else {
            activities.sendChatMessage(ChatGateway.MessageArgs.builder()
                    .chatId(responseMsg.chatId())
                    .replyMessageId(initialMessageId)
                    .messageTemplate("Фильм %s уже в списке")
                    .templateArgs(List.of(resp.movieItem().getFullTitle()))
                    .build());
        }



        activities.startContentSearch(resp.movieItem(), enlistRequest);
    }

    private void addAsWatched(String initialMessageId,
                              ExternalMovieDatabase.ExternalMovieDto movieDto,
                              ChatMessageResponse responseMsg) {
        try {
            // TODO use message timestamp
            LocalDate watchedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            var enlistRequest = EnlistWatchedMovieRequest.builder()
                    .title(movieDto.name())
                    .foreignTitle(movieDto.enName())
                    .year(movieDto.year())
                    .externalId(movieDto.externalId())
                    .watchedAt(watchedAt)
                    .username(responseMsg.username())
                    .build();

            var response = activities.enlistWatchedMovie(enlistRequest);

            activities.sendChatMessage(ChatGateway.MessageArgs.builder()
                    .chatId(responseMsg.chatId())
                    .replyMessageId(initialMessageId)
                    .messageTemplate("Фильм %s добавлен как просмотренный")
                    .templateArgs(List.of(response.fullTitle()))
                    .build());
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw ApplicationFailure.newNonRetryableFailure(
                    "Unable to add as watched due to " + e,
                    "EnlistWatchedMovieFailed",
                    e);
        }
    }

    // TODO Move to Test
    @SneakyThrows
    private static List<ExternalMovieDatabase.ExternalMovieDto> mockResult() {
        return new ObjectMapper().readValue("""
                [
                  {
                    "year": 1991,
                    "name": "Терминатор 2: Судный день",
                    "enName": "Terminator 2: Judgment Day",
                    "externalId": "444",
                    "previewImageUrl": "https://kinopoiskapiunofficial.tech/images/posters/kp_small/444.jpg",
                    "fullName": "Терминатор 2: Судный день (1991, Terminator 2: Judgment Day)"
                  },
                  {
                    "year": 1996,
                    "name": "Терминатор 2 – 3D",
                    "enName": "T2 3-D: Battle Across Time",
                    "externalId": "6299",
                    "previewImageUrl": "https://kinopoiskapiunofficial.tech/images/posters/kp_small/6299.jpg",
                    "fullName": "Терминатор 2 – 3D (1996, T2 3-D: Battle Across Time)"
                  },
                  {
                    "year": 1989,
                    "name": "Терминатор II",
                    "enName": "Terminator II",
                    "externalId": "23300",
                    "previewImageUrl": "https://kinopoiskapiunofficial.tech/images/posters/kp_small/23300.jpg",
                    "fullName": "Терминатор II (1989, Terminator II)"
                  }
                ]""", new TypeReference<>() {
        });
    }
}