package io.kluev.watchlist.infra.config.beans;

import com.google.api.services.sheets.v4.Sheets;
import io.kluev.watchlist.app.EnlistMovieHandler;
import io.kluev.watchlist.app.GetWatchListHandler;
import io.kluev.watchlist.app.LockService;
import io.kluev.watchlist.app.PlayHandler;
import io.kluev.watchlist.domain.MovieRepository;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.SimpleLockService;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import io.kluev.watchlist.infra.googlesheet.GoogleSheetsWatchListRepository;
import io.kluev.watchlist.infra.kinopoisk.KinopoiskClient;
import io.kluev.watchlist.infra.telegrambot.WatchListTGBot;

import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;

@SuppressWarnings("unused")
@Configuration
public class MainBeansConfig {


    @Bean
    public RestTemplate restTemplate() {
        val template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create(restTemplate());
    }

    @Bean
    public NodeRedSeriesRepository nodeRedSeriesRepository(NodeRedIntegrationProperties properties) {
        return new NodeRedSeriesRepository(seriesIdGenerator(), restClient(), properties);
    }

    @Bean
    public GetWatchListHandler getWatchListHandler(SeriesRepository seriesRepository) {
        return new GetWatchListHandler(seriesRepository);
    }

    @Bean
    public PlayHandler playHandler(NodeRedIntegrationProperties properties) {
        return new PlayHandler(restClient(), properties);
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
            EnlistMovieHandler enlistMovieHandler
    ) {
        return new WatchListTGBot(apiKey, allowedUsers, telegramClient, externalMovieDatabase, enlistMovieHandler);
    }

    @Bean
    public EnlistMovieHandler enlistMovieHandler(MovieRepository movieRepository) {
        return new EnlistMovieHandler(movieRepository);
    }
}
