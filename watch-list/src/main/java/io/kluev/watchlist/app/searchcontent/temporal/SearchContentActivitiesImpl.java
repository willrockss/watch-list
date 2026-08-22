package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.JackettGateway;
import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.app.event.ContentSelectedForDownload;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.infra.config.props.SearchContentProperties;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "watch-list-activity-worker")
@Component("searchContentActivitiesBean")
public class SearchContentActivitiesImpl implements SearchContentActivities {

    public static int OK_FOUND_SIZE_THRESHOLD = 3;

    private final SearchContentProperties properties;
    private final JackettGateway jackettGateway;
    private final ChatGateway chatGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<DownloadableContentInfo> findDownloadableContent(MovieItem movie) {
        val year = movie.getYear();
        val possibleYear = Set.of(String.valueOf(year), String.valueOf(year - 1), String.valueOf(year + 1));

        val found = findDownloadableContent(movie, it -> {
            val title = it.getTitle();
            return !title.contains("DVD9") && possibleYear.stream().anyMatch(title::contains);
        });
        return found
                .stream()
                .distinct()
                .sorted(Comparator.comparing(DownloadableContentInfo::getSize).reversed())
                .limit(10)
                .toList();
    }

    @Override
    public void sendSelectContentRequest(String workflowButtonPrefix, List<DownloadableContentInfo> found) {
        chatGateway.sendSelectContentRequest(workflowButtonPrefix, found);
    }

    @Override
    @SneakyThrows
    public String downloadAndSave(DownloadableContentInfo content, MovieItem movie) {
        val torrFileContent = jackettGateway.download(content);
        String filename = switch (properties.getTorrFilenameStrategy()) {
            case EXTERNAL_ID -> movie.getExternalId() + ".torrent";
            case ESCAPED -> torrFileContent.filename();
        };
        val file = Path.of(properties.getTorrFolder(), filename).toFile();

        FileUtils.writeByteArrayToFile(file, torrFileContent.bytes());
        log.info("{} was created", file.getCanonicalFile());

        return file.getAbsolutePath();
    }

    @Override
    public void sendMessage(String chatId, String messageTemplate, String... args) {
        chatGateway.sendMessage(chatId, messageTemplate, args);
    }

    @Override
    public void publishContentSelected(MovieItem movie, String savedFilename) {
        eventPublisher.publishEvent(new ContentSelectedForDownload(movie, savedFilename));
    }

    private @NotNull List<DownloadableContentInfo> findDownloadableContent(MovieItem item, @NotNull Predicate<DownloadableContentInfo> filter) {
        var foundByFullTitle = jackettGateway.query(item.getFullTitle(), filter);
        if (isResultConsideredOk(foundByFullTitle)) {
            return foundByFullTitle;
        }

        List<DownloadableContentInfo> foundByForeignTitle = List.of();
        if (item.hasForeignTitle()) {
            foundByForeignTitle = jackettGateway.query("%s %s".formatted(item.getTitle(), item.getForeignTitle()), filter);
            if (isResultConsideredOk(foundByForeignTitle)) {
                return combine(foundByFullTitle, foundByForeignTitle);
            }
        }
        return combine(foundByFullTitle, foundByForeignTitle, jackettGateway.query(item.getTitle(), filter));
    }

    private boolean isResultConsideredOk(List<DownloadableContentInfo> result) {
        return result.size() > OK_FOUND_SIZE_THRESHOLD;
    }

    @SafeVarargs
    private List<DownloadableContentInfo> combine(@NotNull Collection<DownloadableContentInfo>... results) {
        var res = new ArrayList<DownloadableContentInfo>(results.length);
        for (Collection<DownloadableContentInfo> result : results) {
            res.addAll(result);
        }
        return res;
    }
}