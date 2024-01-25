package io.kluev.watchlist.infra.config.beans;

import io.kluev.watchlist.app.GetWatchListHandler;
import io.kluev.watchlist.app.LockService;
import io.kluev.watchlist.app.PlayHandler;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.SimpleLockService;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

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
}
