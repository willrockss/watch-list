package io.kluev.watchlist.infra.prowlarr;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.ProwlarrProperties;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@Tag("IntegrationTest")
@EnableConfigurationProperties(ProwlarrProperties.class)
@SpringBootTest(
        classes = {ProwlarrRestGateway.class, ProwlarrRestGatewayIntegrationTest.TestConfig.class},
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
class ProwlarrRestGatewayIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.prowlarr.base-url", () -> "http://localhost:" + wm.getPort());
        registry.add("integration.prowlarr.api-key", () -> "test-api-key");
    }

    @Autowired
    private ProwlarrRestGateway sut;

    @SneakyThrows
    @Test
    public void should_query_and_map_search_results() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1/search"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .withQueryParam("query", equalTo("terminator"))
                .withQueryParam("limit", equalTo("40"))
                .withQueryParam("type", equalTo("search"))
                .withQueryParam("categories", equalTo("2000,5000"))
                .willReturn(okForContentType("application/json", """
                        [
                          {
                            "guid": "rutracker.org/t=6376416",
                            "indexer": "RuTracker.org",
                            "title": "Terminator 2: Judgment Day [1991, BDRip 1080p]",
                            "downloadUrl": "http://localhost:%d/4/download?apikey=test-api-key&torrent=ttt"
                          },
                          {
                            "guid": "rutracker.org/t=6376580",
                            "indexer": "RuTracker.org",
                            "title": "Terminator 2: Judgment Day [1991, BDRip 720p]",
                            "magnetUrl": "magnet:?xt=urn:btih:def456&dn=Terminator.2",
                            "size": 19716241408,
                            "seeders": 57,
                            "leechers": 8,
                            "grabs": 7652
                          }
                        ]
                        """.formatted(wm.getPort())))
        );

        // when
        val result = sut.query("terminator");

        // then
        assertThat(result).hasSize(2);

        val first = result.get(0);
        assertThat(first.getGuid()).isEqualTo("rutracker.org/t=6376416");
        assertThat(first.getTitle()).startsWith("Terminator 2: Judgment Day");
        assertThat(first.getLink()).contains("/4/download?apikey=test-api-key&torrent=ttt");

        val second = result.get(1);
        assertThat(second.getLink()).startsWith("magnet:?");
        assertThat(second.getSize()).isEqualTo(19716241408L);
        assertThat(second.getStatistics().getSeeders()).isEqualTo(57);
        assertThat(second.getStatistics().getPeers()).isEqualTo(8);
        assertThat(second.getStatistics().getDownloadedTimes()).isEqualTo(7652);
    }

    @Test
    public void should_apply_filter_predicate() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1/search"))
                .willReturn(okForContentType("application/json", """
                        [
                          {"guid": "g1", "title": "Too small", "size": 100},
                          {"guid": "g2", "title": "Big enough", "size": 1073741824}
                        ]
                        """))
        );

        // when
        val result = sut.query("terminator", (Predicate<DownloadableContentInfo>) info -> info.getSize() > 1024 * 1024);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getGuid()).isEqualTo("g2");
    }

    @SneakyThrows
    @Test
    public void should_download_file_and_use_content_disposition_filename() {
        // given
        val torrentBytes = "fake-torrent-content".getBytes(StandardCharsets.UTF_8);
        wm.stubFor(get(urlPathEqualTo("/4/download"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-bittorrent")
                        .withHeader("Content-Disposition", "attachment; filename=\"Terminator%202%20%5B1991%5D.torrent\"")
                        .withBody(torrentBytes))
        );
        val contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2: Judgment Day [1991]")
                .link("http://localhost:" + wm.getPort() + "/4/download?apikey=test-api-key&torrent=ttt")
                .build();

        // when
        val result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_[1991].torrent");
        assertThat(result.bytes()).isEqualTo(torrentBytes);
        wm.verify(getRequestedFor(urlPathEqualTo("/4/download"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .withQueryParam("torrent", equalTo("ttt")));
    }

    @Test
    public void should_derive_filename_from_title_when_content_disposition_missing() {
        // given
        val torrentBytes = "fake-torrent-content".getBytes(StandardCharsets.UTF_8);
        wm.stubFor(get(urlPathEqualTo("/4/download"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-bittorrent")
                        .withBody(torrentBytes))
        );
        val contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2: Judgment Day [1991]")
                .link("http://localhost:" + wm.getPort() + "/4/download?apikey=test-api-key")
                .build();

        // when
        val result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_Judgment_Day_[1991].torrent");
        assertThat(result.bytes()).isEqualTo(torrentBytes);
    }

    @Test
    public void should_fail_when_body_is_empty() {
        // given
        wm.stubFor(get(urlPathEqualTo("/4/download")).willReturn(ok()));
        val contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2")
                .link("http://localhost:" + wm.getPort() + "/4/download")
                .build();

        // when then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> sut.download(contentInfo));
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestClient restClient() {
            return RestClient.builder().build();
        }
    }
}
