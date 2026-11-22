package io.kluev.watchlist.infra.prowlarr;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.ProwlarrProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * This is not a real test. Just a playground with Prowlarr API
 */
@Disabled
@Tag("IntegrationTest")
@EnableConfigurationProperties(ProwlarrProperties.class)
@SuppressWarnings("unused")
@SpringBootTest(properties = {
        "integration.prowlarr.enabled=true",
        "integration.prowlarr.base-url=http://localhost:9696",
        "integration.prowlarr.api-key=123",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "integration.telegram-bot.session-store-type=NOOP"
}, classes = {ProwlarrRestGateway.class, ProwlarrRestGatewayPlaygroundIT.TestConfig.class})
class ProwlarrRestGatewayPlaygroundIT {

    @Autowired
    private ProwlarrRestGateway prowlarrRestGateway;

    @Test
    public void should_find_content() {
        var query = "Терминатор 2";
        var response = prowlarrRestGateway.query(query);
        System.out.println(response);
        for (DownloadableContentInfo con : response) {
            System.out.println(con.getLink());
        }
        var content = prowlarrRestGateway.download(response.getFirst());
        System.out.println(content);
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestClient restClient() {
            return RestClient.builder().build();
        }
    }
}
