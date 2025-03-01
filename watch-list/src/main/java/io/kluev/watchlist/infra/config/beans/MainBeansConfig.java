package io.kluev.watchlist.infra.config.beans;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.EnlistMovieHandler;
import io.kluev.watchlist.app.EnlistWatchedMovieHandler;
import io.kluev.watchlist.app.GetWatchListHandler;
import io.kluev.watchlist.app.JackettGateway;
import io.kluev.watchlist.app.LockService;
import io.kluev.watchlist.app.PlayHandler;
import io.kluev.watchlist.app.ProgressHandlerV2;
import io.kluev.watchlist.app.downloadcontent.DownloadProcessCoordinator;
import io.kluev.watchlist.app.downloadcontent.QBitClient;
import io.kluev.watchlist.app.searchcontent.SearchContentHandler;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.app.SeriesRepository;
import io.kluev.watchlist.domain.SimpleOffsetWatchDateStrategy;
import io.kluev.watchlist.domain.WatchDateStrategy;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.SimpleLockService;
import io.kluev.watchlist.infra.config.management.PidInfoContributor;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import io.kluev.watchlist.infra.config.props.JackettProperties;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import io.kluev.watchlist.infra.config.props.QBitClientProperties;
import io.kluev.watchlist.infra.config.props.SearchContentProperties;
import io.kluev.watchlist.infra.config.props.TelegramBotProperties;
import io.kluev.watchlist.infra.config.props.VideoServerProperties;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDao;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsWatchListRepository;
import io.kluev.watchlist.infra.jackett.JackettRestGateway;
import io.kluev.watchlist.infra.kinopoisk.KinopoiskClient;
import io.kluev.watchlist.infra.qbit.QBitClientImpl;
import io.kluev.watchlist.infra.telegrambot.NoopTelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.PgTelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.TelegramChatGateway;
import io.kluev.watchlist.infra.telegrambot.TelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.WatchListTGBot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
@Configuration
public class MainBeansConfig {

    /**
     * Will be removed in the future. Use {@link RestClient} only
     */
    @Deprecated
    @Bean
    public RestTemplate restTemplate() {
        val template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    /**
     * This client has small timeout to quickly check remote service availability
     */
    @Bean
    public RestClient restClientChecker(RestClient.Builder builder) {
        val factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(Duration.ofSeconds(2));
        factory.setConnectTimeout(Duration.ofSeconds(2));
        return builder
                .requestFactory(factory)
                .build();
    }

    @Bean
    public NodeRedSeriesRepository nodeRedSeriesRepository(
            RestClient restClient,
            NodeRedIntegrationProperties properties,
            VideoServerProperties videoServerProperties
    ) {
        return new NodeRedSeriesRepository(seriesIdGenerator(), restClient, properties, videoServerProperties);
    }

    @Bean
    public GetWatchListHandler getWatchListHandler(
            NodeRedSeriesRepository seriesRepository,
            MovieRepository movieRepository,
            VideoServerProperties videoServerProperties,
            ExecutorService virtualThreadExecutorService
    ) {
        return new GetWatchListHandler(seriesRepository, movieRepository, videoServerProperties, virtualThreadExecutorService);
    }

    @Bean
    public PlayHandler playHandler(NodeRedIntegrationProperties properties, RestClient restClient) {
        return new PlayHandler(restClient, properties);
    }

    @Bean
    public SeriesIdGenerator seriesIdGenerator() {
        return new SeriesIdGenerator();
    }

    @Bean
    public LockService lockService() {
        return new SimpleLockService();
    }

    @Bean
    public MovieRepository googleSheetsWatchListRepository(
            Sheets sheetsService,
            GoogleSheetProperties properties
    ) {
        return new GoogleSheetsWatchListRepository(sheetsService, properties);
    }

    @Bean
    public ExternalMovieDatabase kinopoiskExternalMovieDatabase(@Value("${integration.kinopoisk.api.key}") String apiKey) {
        return new KinopoiskClient(apiKey);
    }


    @ConditionalOnProperty(name = "integration.telegram-bot.session-store-type", havingValue = "PG", matchIfMissing = true)
    @Bean
    public TelegramSessionStore pgTelegramSessionStore(JdbcClient jdbcClient) {
        return new PgTelegramSessionStore(jdbcClient);
    }

    @ConditionalOnProperty(name = "integration.telegram-bot.session-store-type", havingValue = "NOOP")
    @Bean
    public TelegramSessionStore noopTetelegramSessionStore() {
        return new NoopTelegramSessionStore();
    }

    @Bean
    public TelegramClient okHttpTelegramClient(@Value("${integration.telegram-bot.api.key}") String apiKey) {
        return new OkHttpTelegramClient(apiKey);
    }

    @ConditionalOnProperty(value = "integration.telegram-bot.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public WatchListTGBot watchListTGBot(
            @Value("${integration.telegram-bot.api.key}") String apiKey,
            @Value("${integration.telegram-bot.allowed-users}") Set<String> allowedUsers,
            TelegramClient telegramClient,
            ExternalMovieDatabase externalMovieDatabase,
            EnlistMovieHandler enlistMovieHandler,
            EnlistWatchedMovieHandler enlistWatchedMovieHandler,
            TelegramSessionStore telegramSessionStore,
            ApplicationEventPublisher eventPublisher
    ) {
        return new WatchListTGBot(
                apiKey,
                allowedUsers,
                telegramClient,
                externalMovieDatabase,
                enlistMovieHandler,
                enlistWatchedMovieHandler,
                telegramSessionStore,
                eventPublisher
        );
    }

    @Bean
    public EnlistMovieHandler enlistMovieHandler(MovieRepository movieRepository, ApplicationEventPublisher publisher) {
        return new EnlistMovieHandler(movieRepository, publisher);
    }

    @Bean
    public EnlistWatchedMovieHandler enlistWatchedMovieHandler(MovieRepository movieRepository) {
        return new EnlistWatchedMovieHandler(movieRepository);
    }

    @Bean
    public JackettGateway jackettRestGateway(JackettProperties properties, RestClient restClient) {
        return new JackettRestGateway(properties, restClient);
    }

    @Bean
    public ChatGateway chatGateway(
            TelegramClient telegramClient,
            TelegramSessionStore telegramSessionStore,
            TelegramBotProperties telegramBotProperties
    ) {
        return new TelegramChatGateway(telegramClient, telegramSessionStore, telegramBotProperties);
    }

    @Bean
    public SearchContentHandler searchContentHandler(
            SearchContentProperties properties,
            JackettGateway jackettGateway,
            ChatGateway chatGateway,
            ApplicationEventPublisher eventPublisher
    ) {
        return new SearchContentHandler(properties, jackettGateway, chatGateway, eventPublisher);
    }

    @Bean
    public DownloadContentProcessDao downloadContentProcessDao(JdbcClient jdbcClient) {
        return new DownloadContentProcessDao(jdbcClient);
    }

    @Bean
    public DownloadProcessCoordinator downloadProcessCoordinator(
            DownloadContentProcessDao downloadContentProcessDao,
            QBitClient qBitClient,
            ApplicationEventPublisher eventPublisher
    ) {
        return new DownloadProcessCoordinator(downloadContentProcessDao, qBitClient, eventPublisher);
    }

    @Bean
    public QBitClient qBitClient(RestClient restClient, RestClient restClientChecker, QBitClientProperties properties) {
        return new QBitClientImpl(restClient, properties);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String appName
    ) {
        return registry -> registry
                .config()
                .commonTags("application", appName);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public WatchDateStrategy watchDateStrategy() {
        return new SimpleOffsetWatchDateStrategy(3); // TODO read from config
    }

    @Bean
    public ProgressHandlerV2 progressHandlerV2(
            MovieRepository movieRepository,
            WatchDateStrategy watchDateStrategy,
            Clock clock,
            RestClient restClient,
            NodeRedIntegrationProperties properties,
            LockService lockService,
            SeriesRepository seriesRepository

    ) {
        return new ProgressHandlerV2(movieRepository, watchDateStrategy, clock, restClient, properties, lockService, seriesRepository);
    }

    @Bean
    public ExecutorService virtualThreadExecutorService() {
        val factory = Thread.ofVirtual().name("virtual-thread-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }


    @Bean
    public PidInfoContributor pidInfoContributor() {
        return new PidInfoContributor();
    }
}
