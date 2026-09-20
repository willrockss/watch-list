package io.kluev.watchlist.app.downloadcontent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadFinishedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemDownloadStartedEvent;
import io.kluev.watchlist.app.downloadcontent.event.ContentItemEnqueuedEvent;
import io.kluev.watchlist.app.event.ContentSelectedForDownload;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDao;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDbRecord;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentTaskDbRecordRowMapper;
import io.kluev.watchlist.infra.googlesheet.clientimpl.GoogleSheetsClient;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.unit.DataSize;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.FINISHED;
import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.INITIAL;
import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.PAUSED;
import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@Tag("IntegrationTest")
@Testcontainers
@SpringBootTest(properties = "spring.temporal.start-workers=false")
class DownloadProcessCoordinatorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:14");

    private static final String TORR_HASH = "0123456789abcdef0123456789abcdef01234567";
    private static final String CONTENT_PATH = "/downloads/movie";

    @Autowired
    private DownloadProcessCoordinator coordinator;
    @Autowired
    private DownloadContentProcessDao dao;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private EventRecorder eventRecorder;

    @MockBean
    private QBitClient qBitClient;
    @MockBean
    private MovieRepository movieRepository;
    @MockBean
    private GoogleSheetsClient googleSheetsClient;

    private final Map<ch.qos.logback.classic.Logger, ListAppender<ILoggingEvent>> logAppenders = new HashMap<>();

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM download_content_process").update();
        resetCoordinatorCache();
        eventRecorder.clear();

        when(qBitClient.isAvailable()).thenReturn(true);
        when(qBitClient.getFreeSpaceOnDisk()).thenReturn(DataSize.ofGigabytes(200));
        when(movieRepository.getMoviesReadyToWatch()).thenReturn(List.of());
        when(qBitClient.addTorrPaused(anyString(), any(ContentItemIdentity.class)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));
        when(qBitClient.findByIdTagOrNull(any(ContentItemIdentity.class)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 0, 1024L));
    }

    @AfterEach
    void tearDown() {
        logAppenders.forEach((logger, appender) -> logger.detachAppender(appender));
        logAppenders.clear();
    }

    @Test
    void contentSelected_event_persistsInitialProcess() throws InterruptedException {
        eventPublisher.publishEvent(new ContentSelectedForDownload(
                MovieItem.create("Movie A", "Movie A", 2024, "kinopoisk-t010"),
                "/tmp/t010.torrent"));

        val record = awaitRow("kinopoisk-t010");

        assertThat(record.status()).isEqualTo(INITIAL.name());
        assertThat(record.contentItemIdentity()).isEqualTo("kinopoisk-t010");
        assertThat(record.torrFilePath()).isEqualTo("/tmp/t010.torrent");
    }

    @Test
    void initialProcess_whenGatesPass_singleTickEnqueuesAndStarts() {
        save(process("kinopoisk-t011"));

        coordinator.tick();

        val record = dbRow("kinopoisk-t011");
        assertThat(record.status()).isEqualTo(PROCESSING.name());
        assertThat(record.torrInfoHash()).isEqualTo(TORR_HASH);
        assertThat(record.contentPath()).isEqualTo(CONTENT_PATH);
        verify(qBitClient).addTorrPaused("/tmp/kinopoisk-t011.torrent", new ContentItemIdentity("kinopoisk-t011"));
        verify(qBitClient).start(any(EnqueuedTorr.class));
        assertThat(eventRecorder.enqueued()).hasSize(1);
        assertThat(eventRecorder.started()).hasSize(1);
    }

    @Test
    void initialProcess_whenQuotaReached_isStillEnqueuedPausedButNotStarted() {
        when(movieRepository.getMoviesReadyToWatch())
                .thenReturn(List.of(mock(MovieItem.class), mock(MovieItem.class), mock(MovieItem.class), mock(MovieItem.class)));
        save(process("kinopoisk-t020"));

        coordinator.tick();

        val record = dbRow("kinopoisk-t020");
        assertThat(record.status()).isEqualTo(PAUSED.name());
        assertThat(record.torrInfoHash()).isEqualTo(TORR_HASH);
        assertThat(record.contentPath()).isEqualTo(CONTENT_PATH);
        verify(qBitClient).addTorrPaused(anyString(), any(ContentItemIdentity.class));
        verify(qBitClient, never()).start(any(EnqueuedTorr.class));
        assertThat(eventRecorder.enqueued()).hasSize(1);
        assertThat(eventRecorder.started()).isEmpty();
    }

    @Test
    void initialProcess_whenDiskSpaceInsufficient_isStillEnqueuedPausedButNotStarted() {
        when(qBitClient.getFreeSpaceOnDisk()).thenReturn(DataSize.ofGigabytes(1));
        save(process("kinopoisk-t021"));

        coordinator.tick();

        val record = dbRow("kinopoisk-t021");
        assertThat(record.status()).isEqualTo(PAUSED.name());
        assertThat(record.torrInfoHash()).isEqualTo(TORR_HASH);
        verify(qBitClient).addTorrPaused(anyString(), any(ContentItemIdentity.class));
        verify(qBitClient, never()).start(any(EnqueuedTorr.class));
        assertThat(eventRecorder.enqueued()).hasSize(1);
    }

    @Test
    void pausedProcess_withFailingGate_secondTickStaysPausedWithoutReEnqueueOrRelog() {
        when(qBitClient.getFreeSpaceOnDisk()).thenReturn(DataSize.ofGigabytes(1));
        save(process("kinopoisk-t022"));
        val logs = attachCoordinatorLogs();

        coordinator.tick();
        coordinator.tick();

        val record = dbRow("kinopoisk-t022");
        assertThat(record.status()).isEqualTo(PAUSED.name());
        verify(qBitClient, times(1)).addTorrPaused(anyString(), any(ContentItemIdentity.class));
        verify(qBitClient, never()).start(any(EnqueuedTorr.class));
        assertThat(eventRecorder.enqueued()).hasSize(1);
        assertThat(logs.list.stream()
                .filter(e -> e.getFormattedMessage().contains("disk space to start"))
                .count()).isEqualTo(1);
    }

    @Test
    void whenDiskSpaceQueryFails_treatedAsInsufficient_failSafe_noStart() {
        when(qBitClient.getFreeSpaceOnDisk()).thenThrow(new RuntimeException("boom"));
        save(process("kinopoisk-t023"));

        coordinator.tick();

        val record = dbRow("kinopoisk-t023");
        assertThat(record.status()).isEqualTo(PAUSED.name());
        verify(qBitClient, never()).start(any(EnqueuedTorr.class));
    }

    @Test
    void whenQBitUnavailable_wholeTickSkipped() {
        when(qBitClient.isAvailable()).thenReturn(false);
        save(process("kinopoisk-t024"));

        coordinator.tick();

        val record = dbRow("kinopoisk-t024");
        assertThat(record.status()).isEqualTo(INITIAL.name());
        verify(qBitClient, never()).addTorrPaused(anyString(), any(ContentItemIdentity.class));
        verify(qBitClient, never()).start(any(EnqueuedTorr.class));
        assertThat(eventRecorder.all()).isEmpty();
    }

    @Test
    void processingProcess_withFinishedTorrent_movesToFinishedAndPublishes() {
        when(qBitClient.findByIdTagOrNull(any(ContentItemIdentity.class)))
                .thenReturn(new EnqueuedTorr(TORR_HASH, CONTENT_PATH, 50, 1024L));
        save(processingProcess("kinopoisk-t030"));

        coordinator.tick();

        assertThat(dbRow("kinopoisk-t030").status()).isEqualTo(FINISHED.name());
        assertThat(eventRecorder.finished()).hasSize(1);
    }

    @Test
    void processingProcess_withRunningTorrent_staysProcessing_noEvent() {
        save(processingProcess("kinopoisk-t031"));

        coordinator.tick();

        assertThat(dbRow("kinopoisk-t031").status()).isEqualTo(PROCESSING.name());
        assertThat(eventRecorder.all()).isEmpty();
    }

    private DownloadContentProcess process(String identity) {
        return DownloadContentProcess.builder()
                .contentItemIdentity(new ContentItemIdentity(identity))
                .torrFilePath("/tmp/" + identity + ".torrent")
                .build();
    }

    private DownloadContentProcess processingProcess(String identity) {
        return DownloadContentProcess.builder()
                .contentItemIdentity(new ContentItemIdentity(identity))
                .torrFilePath("/tmp/" + identity + ".torrent")
                .status(PROCESSING)
                .torrInfoHash(TORR_HASH)
                .contentPath(CONTENT_PATH)
                .build();
    }

    private void save(DownloadContentProcess process) {
        dao.save(process);
    }

    private DownloadContentProcessDbRecord dbRow(String identity) {
        return jdbcClient.sql("SELECT * FROM download_content_process WHERE content_item_identity = :identity")
                .param("identity", identity)
                .query(DownloadContentTaskDbRecordRowMapper.INSTANCE)
                .optional()
                .orElseThrow(() -> new AssertionError("No download_content_process row for " + identity));
    }

    private DownloadContentProcessDbRecord awaitRow(String identity) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            val row = jdbcClient.sql("SELECT * FROM download_content_process WHERE content_item_identity = :identity")
                    .param("identity", identity)
                    .query(DownloadContentTaskDbRecordRowMapper.INSTANCE)
                    .optional();
            if (row.isPresent()) {
                return row.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No download_content_process row appeared within timeout for " + identity);
    }

    private ListAppender<ILoggingEvent> attachCoordinatorLogs() {
        val logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DownloadProcessCoordinator.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        val appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logAppenders.put(logger, appender);
        return appender;
    }

    private void resetCoordinatorCache() {
        val target = AopTestUtils.getUltimateTargetObject(coordinator);
        try {
            Field cacheField = DownloadProcessCoordinator.class.getDeclaredField("activeProcessesCache");
            cacheField.setAccessible(true);
            Object value = cacheField.get(target);
            if (value == null) {
                cacheField.set(target, new ArrayList<DownloadContentProcess>());
            } else {
                ((List<?>) value).clear();
            }
            Field nextUpdateField = DownloadProcessCoordinator.class.getDeclaredField("nextCacheUpdateAfterTimestampMillis");
            nextUpdateField.setAccessible(true);
            nextUpdateField.setLong(target, 0L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to reset coordinator cache", e);
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }

        @Bean(name = "taskScheduler")
        TaskScheduler taskScheduler() {
            return new NeverRunTaskScheduler();
        }
    }

    static class EventRecorder {
        private final List<Object> events = Collections.synchronizedList(new ArrayList<>());

        @EventListener({
                ContentItemEnqueuedEvent.class,
                ContentItemDownloadStartedEvent.class,
                ContentItemDownloadFinishedEvent.class
        })
        public void record(Object event) {
            events.add(event);
        }

        void clear() {
            events.clear();
        }

        List<ContentItemEnqueuedEvent> enqueued() {
            return events.stream()
                    .filter(ContentItemEnqueuedEvent.class::isInstance)
                    .map(ContentItemEnqueuedEvent.class::cast)
                    .toList();
        }

        List<ContentItemDownloadStartedEvent> started() {
            return events.stream()
                    .filter(ContentItemDownloadStartedEvent.class::isInstance)
                    .map(ContentItemDownloadStartedEvent.class::cast)
                    .toList();
        }

        List<ContentItemDownloadFinishedEvent> finished() {
            return events.stream()
                    .filter(ContentItemDownloadFinishedEvent.class::isInstance)
                    .map(ContentItemDownloadFinishedEvent.class::cast)
                    .toList();
        }

        List<Object> all() {
            return List.copyOf(events);
        }
    }

static class NeverRunTaskScheduler implements TaskScheduler {
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return never();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            return never();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return never();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return never();
        }

        private ScheduledFuture<?> never() {
            return executor.schedule(() -> { }, 365, TimeUnit.DAYS);
        }
    }
}