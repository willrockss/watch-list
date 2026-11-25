package io.kluev.watchlist.app.downloadcontent;

import io.kluev.watchlist.infra.config.props.QBitClientProperties;
import io.kluev.watchlist.infra.qbit.QBitClientImpl;
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

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final String TEST_MAGNET_LINK = "magnet:?xt=urn:btih:B1239D600B39274618B5ACD7755EABD00BAB5B04&tr=http%3A%2F%2Aaa4.t-org.org%2Fann%3Fmagnet";
    @Test
    public void test_add() {
        var torrPath = "/home/alex/Downloads/torr/Головоломка_2_Inside_Out_2_(Келси_Манн)_[2024_комедия_приключения_семейный_WEBRip]_тизер.torrent";
        var torr = qBitClient.addTorrByFilePaused(torrPath, new ContentItemIdentity("inside_out_2"));
        assertThat(torr).isNotNull();
        assertThat(torr.infoHash()).isNotNull();
        assertThat(torr.contentPath()).isNotNull();
    }

    @Test
    public void test_add_magnet() {
        var torr = qBitClient.addTorrByLinkPaused(TEST_MAGNET_LINK, new ContentItemIdentity("inside_out_2_magnet"));
        assertThat(torr).isNotNull();
        assertThat(torr.infoHash()).isNotNull();
        assertThat(torr.contentPath()).isNotNull();
    }

    @Test
    public void test_start() {
        var found = qBitClient.findByIdTagOrNull(new ContentItemIdentity("inside_out_2"));
        assertThat(found).isNotNull();
        qBitClient.start(found);
    }

    @Test
    public void find_torr() {
        var found = qBitClient.findByIdTagOrNull(new ContentItemIdentity("inside_out_2"));
        assertThat(found).isNotNull();
    }

    @Test
    public void delete() {
        var found = qBitClient.findByIdTagOrNull(new ContentItemIdentity("inside_out_2"));
        assert found != null;
        qBitClient.deleteWithContent(found);
    }

    @Test
    public void test_available() {
        var isAvailable = qBitClient.isAvailable();
        Assertions.assertThat(isAvailable).isTrue();
    }

    @Test
    public void test_free_space_on_disk() {
        var freeSpace = qBitClient.getFreeSpaceOnDisk();
        Assertions.assertThat(freeSpace).isNotNull();
        Assertions.assertThat(freeSpace.toBytes()).isPositive();
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