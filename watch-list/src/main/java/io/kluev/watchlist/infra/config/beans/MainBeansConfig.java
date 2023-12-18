package io.kluev.watchlist.infra.config.beans;

import io.kluev.watchlist.app.GetWatchListHandler;
import io.kluev.watchlist.app.PlayHandler;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.NodeRedSeriesRepository;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MainBeansConfig {


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create(restTemplate());
    }

    @Bean
    public NodeRedSeriesRepository nodeRedSeriesRepository(NodeRedIntegrationProperties properties) {
        return new NodeRedSeriesRepository(restClient(), properties);
    }

    @Bean
    public GetWatchListHandler getWatchListHandler(SeriesRepository seriesRepository) {
        return new GetWatchListHandler(seriesRepository);
    }

    @Bean
    public PlayHandler playHandler(NodeRedIntegrationProperties properties) {
        return new PlayHandler(restClient(), properties);
    }
}
