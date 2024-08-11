package io.kluev.watchlist.app.downloadcontent;

import io.kluev.watchlist.app.event.ContentSelectedForDownload;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class DownloadProcessCoordinator {
    public static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final DownloadContentProcessDao downloadContentProcessDao;

    private final List<DownloadContentProcess> newProcesses = Collections.synchronizedList(new ArrayList<>());
    private final List<DownloadContentProcess> activeProcessesCache = new ArrayList<>();
    private long nextCacheUpdateAfterTimestampMillis = 0L;

    @Async
    @EventListener(ContentSelectedForDownload.class)
    public void handle(ContentSelectedForDownload event) {
        log.info("Going to create download process for {}", event.movieItem());
        val process = DownloadContentProcess
                .builder()
                .contentItemIdentity(new ContentItemIdentity(event.movieItem().getExternalId()))
                .torrFilePath(event.torrFilename())
                .build();

        downloadContentProcessDao.save(process);
        newProcesses.add(process);
        log.info("Created download process {} for {}", process, event.movieItem());
    }


    @Scheduled(fixedDelay = 15_000)
    public void tick() {
        refreshProcessesCacheIfRequired();

        for (val process : activeProcessesCache) {
            // TODO Implement
        }

    }

    private void refreshProcessesCacheIfRequired() {
        val needToReloadCache = nextCacheUpdateAfterTimestampMillis < System.currentTimeMillis();
        if (needToReloadCache) {
            nextCacheUpdateAfterTimestampMillis = System.currentTimeMillis() + CACHE_TTL_MILLIS;
            activeProcessesCache.clear();
            activeProcessesCache.addAll(downloadContentProcessDao.getActive());
        } else {
            activeProcessesCache.addAll(newProcesses);
        }

        newProcesses.clear();
    }


}
