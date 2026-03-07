package io.kluev.watchlist.app.searchmovie;

import dev.restate.sdk.WorkflowContext;
import dev.restate.sdk.annotation.Workflow;
import dev.restate.sdk.springboot.RestateWorkflow;
import dev.restate.serde.TypeRef;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.ExternalMovieDto;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;

@RequiredArgsConstructor
@RestateWorkflow
public class SearchMovieWorkflow {

    private final ExternalMovieDatabase externalMovieDatabase;

    private static final TypeRef<List<ExternalMovieDto>> LIST_RES_REF = new TypeRef<>(){};

    @Workflow
    public SearchMovieResponse run(WorkflowContext ctx, SearchMovieRequest req) {
        val foundMovies = ctx.run("searchInExternalMovieDb", LIST_RES_REF, () ->
                externalMovieDatabase.find(req.query())
        );
        // TODO send message to chat
        // TODO wait for response
        return new SearchMovieResponse(foundMovies);
    }
}
