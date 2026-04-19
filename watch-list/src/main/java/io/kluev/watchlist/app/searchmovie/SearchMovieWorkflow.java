package io.kluev.watchlist.app.searchmovie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.restate.client.Client;
import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.SharedWorkflowContext;
import dev.restate.sdk.WorkflowContext;
import dev.restate.sdk.annotation.Shared;
import dev.restate.sdk.annotation.Workflow;
import dev.restate.sdk.common.DurablePromiseKey;
import dev.restate.sdk.common.StateKey;
import dev.restate.sdk.common.TerminalException;
import dev.restate.sdk.springboot.RestateWorkflow;
import dev.restate.serde.TypeRef;
import io.kluev.watchlist.app.EnlistMovieRequest;
import io.kluev.watchlist.app.EnlistWatchedMovieHandler;
import io.kluev.watchlist.app.EnlistWatchedMovieRequest;
import io.kluev.watchlist.app.KeyValueStorage;
import io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObjectClient;
import io.kluev.watchlist.app.chat.CallbackCommand;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.kluev.watchlist.app.ExternalMovieDatabase;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestateWorkflow // TODO Rewrite to VirtualObject
public class SearchMovieWorkflow {

    private final ExternalMovieDatabase externalMovieDatabase;
    private final ChatGateway chatGateway;
    private final EnlistWatchedMovieHandler enlistWatchedMovieHandler;
    private final KeyValueStorage keyValueStorage;
    private final Client restateClient;

    private static final TypeRef<List<ExternalMovieDatabase.ExternalMovieDto>> LIST_RES_REF = new TypeRef<>(){};
    private static final StateKey<Integer> CURRENT_INDEX =
            StateKey.of("current_index", Integer.class);
    private static final StateKey<Integer> CALLBACK_COUNTER =
            StateKey.of("callback_counter", Integer.class);

    private static final String COMMAND_PROMISE_NAME_TEMPLATE = "command-%s";

    @Workflow
    public void run(WorkflowContext ctx, SearchMovieRequest req) {
        val foundMovies = ctx.run("searchInExternalMovieDb", LIST_RES_REF, () ->
                externalMovieDatabase.find(req.query())
        );

        if (foundMovies.isEmpty()) {
            ctx.run(() -> chatGateway.sendMessage(req.chatId(), "По запросу '%s' ничего не найдено", req.query()));
            return;
        }

        while (true) {
            int index = ctx.get(CURRENT_INDEX).orElse(0);

            if (index < 0 || index >= foundMovies.size()) {
                throw new TerminalException("Invalid index");
            }

            val currentMovie = foundMovies.get(index);
            val searchResultMessage = ChatGateway.MessageArgs.builder()
                    .chatId(req.chatId())
                    .messageTemplate("%s\n%s")
                    .templateArgs(List.of(currentMovie.getFullName(), currentMovie.previewImageUrl()))
                    .buttons(List.of(
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Добавить в список")
                                    .action(getShortenedLink(ctx, "add_" + index))
                                    .build()
                            ),
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Добавить просмотренным")
                                    .action(getShortenedLink(ctx, "watched_" + index))
                                    .build()
                            ),
                            List.of(ChatGateway.CommandButton.builder()
                                    .caption("Ещё (%s/%s)".formatted((index + 2), foundMovies.size()))
                                    .action(getShortenedLink(ctx, "next_" + (index + 1)))
                                    .condition(() -> (index < foundMovies.size() - 1))
                                    .build())
                    ))
                    .build();
            ctx.run(() -> {
                try {
                    chatGateway.sendMessage(searchResultMessage);
                } catch (Exception e) { // TODO add proper Retry Policy
                    throw new TerminalException("Unable to send a message due to " + e.getMessage());
                }
            });

            Integer callbackCounter = ctx.get(CALLBACK_COUNTER).orElse(0);
            CallbackCommand callbackCommand = ctx.promise(getCurrentCallbackKey(callbackCounter)).future().await();
            ctx.set(CALLBACK_COUNTER, callbackCounter + 1);
            log.debug("Going to process callback command {}", callbackCommand.command());

            // TODO wrap to Specifications
            String[] parts = callbackCommand.command().split("_", 2);

            String action = parts[0];
            int actionMovieIndex = Integer.parseInt(parts[1]);
            switch (action) {
                case "next" -> {
                    if (index >= actionMovieIndex) {
                        log.warn("Invalid next command. Going to put next index to {}, but current is {}", actionMovieIndex, index);
                        ctx.set(CALLBACK_COUNTER, callbackCounter + 1);
                    } else {
                        ctx.set(CURRENT_INDEX, actionMovieIndex);
                    }
                }
                case "add" -> {
                    ctx.run(() -> addToWatchList(ctx, req.messageId(), foundMovies.get(actionMovieIndex), callbackCommand.response()));
                    cleanUp();
                    return;
                }
                case "watched" -> {
                    ctx.run(() -> addAsWatched(req.messageId(), foundMovies.get(actionMovieIndex), callbackCommand.response()));
                    cleanUp();
                    return;
                }
                default -> log.error("Unknown action {}. Try again", action);
            }
        }
    }

    private String getShortenedLink(WorkflowContext ctx, String command) {
        // TODO use Restate SDK to get proper name dynamically
        String rawCommand = "@|%s|%s|processUserCommand|%s".formatted(this.getClass().getSimpleName(), ctx.key(), command);
        // TODO remember key for cleanUp method
        return "⤓|" + keyValueStorage.withRandomKey(rawCommand);
    }

    private void cleanUp() {
        // TODO remove short link keys
    }

    @Shared
    public String processUserCommand(SharedWorkflowContext ctx, CallbackCommand command) {
        val callbackCounter = ctx.get(CALLBACK_COUNTER).orElse(0);
        val callbackKey = getCurrentCallbackKey(callbackCounter);
        ctx.promiseHandle(callbackKey).resolve(command);
        return "ok";
    }

    private DurablePromiseKey<CallbackCommand> getCurrentCallbackKey(Integer callbackCounter) {
        val promiseName = COMMAND_PROMISE_NAME_TEMPLATE.formatted(callbackCounter);
        return DurablePromiseKey.of(promiseName, CallbackCommand.class);
    }

    private void addToWatchList(ObjectContext ctx, String initialMessageId, ExternalMovieDatabase.ExternalMovieDto movieDto, ChatMessageResponse responseMsg) {
        val enlistClient = EnlistMovieVirtualObjectClient.fromClient(restateClient, movieDto.externalId());

        val enlistRequest = EnlistMovieRequest
                .builder()
                .title(movieDto.name())
                .foreignTitle(movieDto.enName())
                .year(movieDto.year())
                .externalId(movieDto.externalId())
                .username(responseMsg.username())
                .build();

        val resp = enlistClient.addToWatchList(enlistRequest);

        ctx.run(() ->
                // TODO send to all users
                chatGateway.sendMessage(ChatGateway.MessageArgs.builder()
                        .chatId(responseMsg.chatId())
                        .replyMessageId(initialMessageId)
                        .messageTemplate("Фильм %s добавлен в список")
                        .templateArgs(List.of(resp.fullTitle()))
                        .build()));
    }

    // TODO move to another Workflow
    private void addAsWatched(String initialMessageId, ExternalMovieDatabase.ExternalMovieDto movieDto, ChatMessageResponse responseMsg) {
        try {
            var enlistRequest = EnlistWatchedMovieRequest
                    .builder()
                    .title(movieDto.name())
                    .foreignTitle(movieDto.enName())
                    .year(movieDto.year())
                    .externalId(movieDto.externalId())
                    .watchedAt(LocalDate.now())
                    .username(responseMsg.username())
                    .build();

            var response = enlistWatchedMovieHandler.handle(enlistRequest);

            // TODO send to all users
            chatGateway.sendMessage(ChatGateway.MessageArgs.builder()
                    .chatId(responseMsg.chatId())
                    .replyMessageId(initialMessageId)
                    .messageTemplate("Фильм %s добавлен как просмотренный")
                    .templateArgs(List.of(response.fullTitle()))
                    .build());
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new TerminalException("Unable to add as watched due to " + e);
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
