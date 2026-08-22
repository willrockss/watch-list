package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.domain.MovieItem;
import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface SearchContentActivities {

    List<DownloadableContentInfo> findDownloadableContent(MovieItem movie);

    void sendSelectContentRequest(String workflowButtonPrefix, List<DownloadableContentInfo> found);

    String downloadAndSave(DownloadableContentInfo content, MovieItem movie);

    void sendMessage(String chatId, String messageTemplate, String... args);

    void publishContentSelected(MovieItem movie, String savedFilename);
}