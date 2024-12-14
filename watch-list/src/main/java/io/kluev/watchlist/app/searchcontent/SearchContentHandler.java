package io.kluev.watchlist.app.searchcontent;

import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.ChatMessageResponse;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.FileContent;
import io.kluev.watchlist.app.JackettGateway;
import io.kluev.watchlist.app.event.ContentSelectedForDownload;
import io.kluev.watchlist.common.utils.NumberUtils;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import io.kluev.watchlist.infra.config.props.SearchContentProperties;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class SearchContentHandler {

    public static int OK_FOUND_SIZE_THRESHOLD = 3;

    private final SearchContentProperties properties;
    private final JackettGateway jackettGateway;
    private final ChatGateway chatGateway;
    private final ApplicationEventPublisher eventPublisher;

    // TODO move to storage
    private final Map<UUID, SearchContentSaga> sagaById = new ConcurrentHashMap<>();

    @Async
    @EventListener(MovieEnlisted.class)
    public void handle(MovieEnlisted event) {
        val saga = SearchContentSaga.create(event.movie());
        sagaById.put(saga.getSagaId(), saga);

        val found = findDownloadableContext(event.movie());
        val top10HighQuality = found
                .stream()
                // TODO Add proper filter, sorter based on strategy
                .filter(it -> !it.getTitle().contains("DVD9"))
                .distinct()
                .sorted(Comparator.comparing(DownloadableContentInfo::getSize).reversed())
                .limit(10)
                .toList();

        saga.setFound(top10HighQuality);
        chatGateway.sendSelectContentRequest(saga.getSagaId(), top10HighQuality);
    }

    private @NonNull List<DownloadableContentInfo> findDownloadableContext(MovieItem item) {
        var foundByFullTitle = jackettGateway.query(item.getFullTitle());
        if (isResultConsideredOk(foundByFullTitle)) {
            return foundByFullTitle;
        }

        List<DownloadableContentInfo> foundByForeignTitle = List.of();
        if (item.hasForeignTitle()) {
            foundByForeignTitle = jackettGateway.query("%s %s".formatted(item.getTitle(), item.getForeignTitle()));
            if (isResultConsideredOk(foundByForeignTitle)) {
                return combine(foundByFullTitle, foundByForeignTitle);
            }
        }
        return combine(foundByFullTitle, foundByForeignTitle, jackettGateway.query(item.getTitle()));
    }

    // TODO Use Specification instead
    private boolean isResultConsideredOk(List<DownloadableContentInfo> result) {
        return result.size() > OK_FOUND_SIZE_THRESHOLD;
    }

    // TODO use Composite collection
    @SafeVarargs
    private List<DownloadableContentInfo> combine(@NotNull Collection<DownloadableContentInfo>... results) {
        var res = new ArrayList<DownloadableContentInfo>(results.length);
        for (Collection<DownloadableContentInfo> result : results) {
            res.addAll(result);
        }
        return res;
    }

    @Async
    @EventListener(ChatMessageResponse.class)
    public void handleResponse(ChatMessageResponse rawResponse) {
        val resp = SearchContentSagaResponse.parseOrNull(rawResponse);
        if (resp == null) {
            log.debug("Unable to parse response {}. Ignore", rawResponse);
            return;
        }

        val saga = sagaById.get(resp.sagaId());
        if (saga == null) {
            log.error("Unable to find saga by id {}. Ignore", resp.sagaId());
            return;
        }

        val selectedContent = findSelectedDownloadableContentInfoOrNull(saga, resp);
        val torrFileContent = jackettGateway.download(selectedContent);
        val savedFilename = save(torrFileContent);
        chatGateway.sendMessage(rawResponse.chatId(), "`file://%s` был успешно скачан", savedFilename);
        eventPublisher.publishEvent(new ContentSelectedForDownload(saga.getMovieItem(), savedFilename));
    }

    private DownloadableContentInfo findSelectedDownloadableContentInfoOrNull(
            SearchContentSaga saga,
            SearchContentSagaResponse resp
    ) {
        // TODO check for null
        val selectedIndex = NumberUtils.parseOrNull(resp.selectedContent()) - 1;
        if (selectedIndex < 0 || selectedIndex >= saga.getFound().size()) {
            log.error("Invalid selected index {}. Found count {}. Ignore", selectedIndex, saga.getFound().size());
            return null;
        }
        return saga.getFound().get(selectedIndex);
    }

    @SneakyThrows
    private String save(FileContent torrFileContent) {
        val file = Path.of(properties.getTorrFolder(), torrFileContent.filename()).toFile();

        FileUtils.writeByteArrayToFile(file, torrFileContent.bytes());
        log.info("{} was created", file.getCanonicalFile());

        return file.getAbsolutePath();
    }

    @Getter
    @RequiredArgsConstructor
    public static class SearchContentSaga {
        private final UUID sagaId;
        private final MovieItem movieItem;
        private final OffsetDateTime createdAt;

        @Setter
        private List<DownloadableContentInfo> found;

        private static SearchContentSaga create(MovieItem movieItem) {
            return new SearchContentSaga(UUID.randomUUID(), movieItem, OffsetDateTime.now() /* TODO use Clock*/);
        }
    }
}
