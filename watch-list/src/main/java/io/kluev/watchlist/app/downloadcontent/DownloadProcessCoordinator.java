package io.kluev.watchlist.app.downloadcontent;

import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadFinishedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemEnqueuedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadStartedEvent;
import io.kluev.watchlist.app.event.ContentSelectedForDownload;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class DownloadProcessCoordinator {
    public static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final DownloadContentProcessDao downloadContentProcessDao;
    private final QBitClient qBitClient;
    private final ApplicationEventPublisher eventPublisher;

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
        markCacheExpired();
        log.info("Created download process {} for {}", process, event.movieItem());
    }

    @Scheduled(fixedDelay = 15_000)
    public void tick() {
        log.debug("tick");

        if (!qBitClient.isAvailable()) {
            log.warn("QBit is not running. Do nothing");
            return;
        }

        refreshProcessesCacheIfRequired();

        if (activeProcessesCache.isEmpty()) {
            log.debug("No active download processes. Do nothing");
            return;
        }

        enqueueNewAsPaused();

        val process = activeProcessesCache.getFirst();
        val status = process.getStatus();
        switch (status) {
            case INITIAL -> {
                log.info("Enqueue and starting download process {}", process);
                process.enqueuePaused(qBitClient);
                process.start(qBitClient);
                downloadContentProcessDao.save(process);
                eventPublisher.publishEvent(new ContentItemDownloadStartedEvent(
                        process.getContentItemIdentity(), process.getContentPath()
                ));
            }
            case PAUSED -> {
                log.info("Start download process {}", process);
                process.start(qBitClient);
                downloadContentProcessDao.save(process);
                eventPublisher.publishEvent(new ContentItemDownloadStartedEvent(
                        process.getContentItemIdentity(), process.getContentPath()
                ));
            }
            case PROCESSING -> {
                log.info("Handle processing process {}", process);
                val finished = process.checkFinished(qBitClient);
                if (finished) {
                    downloadContentProcessDao.save(process);
                    eventPublisher.publishEvent(new ContentItemDownloadFinishedEvent(process.getContentItemIdentity()));
                    markCacheExpired();
                }
            }
        }
    }

    private void enqueueNewAsPaused() {
        activeProcessesCache.stream().filter(DownloadContentProcess::hasInitialStatus).forEach(it -> {
            it.enqueuePaused(qBitClient);
            downloadContentProcessDao.save(it);
            eventPublisher.publishEvent(new ContentItemEnqueuedEvent(it.getContentItemIdentity()));
        });
    }

    private void refreshProcessesCacheIfRequired() {
        val needToReloadCache = nextCacheUpdateAfterTimestampMillis < System.currentTimeMillis();
        if (!needToReloadCache) {
            log.debug("Cache is ok. Do nothing");
            return;
        }

        nextCacheUpdateAfterTimestampMillis = System.currentTimeMillis() + CACHE_TTL_MILLIS;
        val wasEmpty = activeProcessesCache.isEmpty();

        activeProcessesCache.clear();
        activeProcessesCache.addAll(downloadContentProcessDao.getActive());
        activeProcessesCache.sort(Comparator.comparing(DownloadContentProcess::getCreatedAt));

        if (!wasEmpty && !activeProcessesCache.isEmpty()) {
            log.info("Cache is reloaded. Current {}", activeProcessesCache);
        }
    }

    private void markCacheExpired() {
        nextCacheUpdateAfterTimestampMillis = 0L;
    }
}
