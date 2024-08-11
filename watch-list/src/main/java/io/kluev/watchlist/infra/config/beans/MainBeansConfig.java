package io.kluev.watchlist.infra.config.beans;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.app.*;
import io.kluev.watchlist.app.downloadcontent.DownloadProcessCoordinator;
import io.kluev.watchlist.app.searchcontent.SearchContentHandler;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.SimpleLockService;
import io.kluev.watchlist.infra.config.props.*;
import io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDao;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsWatchListRepository;
import io.kluev.watchlist.infra.jackett.JackettRestGateway;
import io.kluev.watchlist.infra.kinopoisk.KinopoiskClient;
import io.kluev.watchlist.infra.telegrambot.TelegramChatGateway;
import io.kluev.watchlist.infra.telegrambot.PgTelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.NoopTelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.TelegramSessionStore;
import io.kluev.watchlist.infra.telegrambot.WatchListTGBot;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Set;

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

    @Bean
    public NodeRedSeriesRepository nodeRedSeriesRepository(NodeRedIntegrationProperties properties, RestClient restClient) {
        return new NodeRedSeriesRepository(seriesIdGenerator(), restClient, properties);
    }

    @Bean
    public GetWatchListHandler getWatchListHandler(SeriesRepository seriesRepository) {
        return new GetWatchListHandler(seriesRepository);
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
            TelegramSessionStore telegramSessionStore,
            ApplicationEventPublisher eventPublisher
    ) {
        return new WatchListTGBot(
                apiKey,
                allowedUsers,
                telegramClient,
                externalMovieDatabase,
                enlistMovieHandler,
                telegramSessionStore,
                eventPublisher
        );
    }

    @Bean
    public EnlistMovieHandler enlistMovieHandler(MovieRepository movieRepository, ApplicationEventPublisher publisher) {
        return new EnlistMovieHandler(movieRepository, publisher);
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
    public DownloadProcessCoordinator downloadProcessCoordinator(DownloadContentProcessDao downloadContentProcessDao) {
        return new DownloadProcessCoordinator(downloadContentProcessDao);
    }
}
