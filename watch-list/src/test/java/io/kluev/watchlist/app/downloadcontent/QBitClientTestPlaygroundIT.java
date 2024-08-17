package io.kluev.watchlist.app.downloadcontent;

import io.kluev.watchlist.infra.config.props.QBitClientProperties;
import io.kluev.watchlist.infra.qbit.QBitClientImpl;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Disabled
@Tag("IntegrationTest")
@EnableConfigurationProperties(value = QBitClientProperties.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                QBitClientTestPlaygroundIT.TestConfig.class,
                RestClient.Builder.class,
                QBitClientImpl.class
        },
        properties = {
                "integration.qbit.url=http://localhost:9090"
        }
)
class QBitClientTestPlaygroundIT {

    @Autowired
    private QBitClient qBitClient;

    @Test
    public void test_add() {
        val torrPath = "/home/alex/Downloads/torr/Головоломка_2_Inside_Out_2_(Келси_Манн)_[2024_комедия_приключения_семейный_WEBRip]_тизер.torrent";
        val torr = qBitClient.addTorr(torrPath, new ContentItemIdentity("inside_out_2"));
        Assertions.assertThat(torr).isNotNull();
    }

    @Test
    public void find_torr() {
        val found = qBitClient.findByIdTagOrNull(new ContentItemIdentity("inside_out_2"));
        Assertions.assertThat(found).isNotNull();
    }

    @Test
    public void delete() {
        val found = qBitClient.findByIdTagOrNull(new ContentItemIdentity("inside_out_2"));
        assert found != null;
        qBitClient.deleteWithContent(found);
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestClient restClient(RestClient.Builder builder) {
            return builder
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .build();
        }

        @Bean
        public RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

}