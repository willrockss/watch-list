package io.kluev.watchlist.domain;

import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadFinishedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadStartedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemEnqueuedEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface MovieRepository {

    void enlist(MovieItem movieItem);
    void enlistWatched(MovieItem movieItem, LocalDate watchedAt);
    List<MovieItem> getMoviesReadyToWatch();
    void markWatched(String kinopoiskId, LocalDate watchedAt);
    void markAsEnqueued(ContentItemEnqueuedEvent contentItemEnqueuedEvent) throws IOException;
    void markAsStarted(ContentItemDownloadStartedEvent contentItemDownloadStartedEvent) throws IOException;
    void markAsFinished(ContentItemDownloadFinishedEvent contentItemDownloadFinishedEvent) throws IOException;

}
