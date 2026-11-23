package io.kluev.watchlist.infra.jacred;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.JacRedProperties;
import lombok.SneakyThrows;
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
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("IntegrationTest")
@EnableConfigurationProperties(JacRedProperties.class)
@SpringBootTest(
        classes = {JacRedRestGateway.class, JacRedRestGatewayIntegrationTest.TestConfig.class},
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
class JacRedRestGatewayIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.jacred.base-url", () -> "http://localhost:" + wm.getPort());
        registry.add("integration.jacred.api-key", () -> "test-api-key");
    }

    @Autowired
    private JacRedRestGateway sut;

    @SneakyThrows
    @Test
    public void should_query_and_map_search_results() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .withQueryParam("search", equalTo("terminator"))
                .withQueryParam("sort", equalTo("pir"))
                .withQueryParam("tracker", equalTo("rutracker,korsars,nnmclub,bitru"))
                .willReturn(okForContentType("application/json", """
                        [
                          {
                            "tracker": "rutracker",
                            "title": "Terminator 2: Judgment Day [1991, BDRip 1080p]",
                            "size": 10709966848,
                            "url": "http://localhost:%d/api/v1.0/torrents/download?apikey=test-api-key&id=6376416",
                            "magnet": "magnet:?xt=urn:btih:abc123&dn=Terminator.2",
                            "sid": 15,
                            "pir": 8
                          },
                          {
                            "tracker": "rutracker",
                            "title": "Terminator 2: Judgment Day [1991, BDRip 720p]",
                            "size": 5368709120,
                            "url": null,
                            "magnet": "magnet:?xt=urn:btih:def456&dn=Terminator.2.720p",
                            "sid": 57,
                            "pir": 8
                          }
                        ]
                        """.formatted(wm.getPort())))
        );

        // when
        var result = sut.query("terminator");

        // then
        wm.verify(getRequestedFor(urlPathEqualTo("/api/v1.0/torrents"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .withQueryParam("search", equalTo("terminator"))
                .withQueryParam("sort", equalTo("pir"))
                .withQueryParam("tracker", equalTo("rutracker,korsars,nnmclub,bitru")));

        assertThat(result).hasSize(2);

        var first = result.getFirst();
        assertThat(first.getGuid()).contains("/api/v1.0/torrents/download?apikey=test-api-key&id=6376416");
        assertThat(first.getTitle()).startsWith("Terminator 2: Judgment Day");
        assertThat(first.getLink()).contains("/api/v1.0/torrents/download?apikey=test-api-key&id=6376416");
        assertThat(first.getSize()).isEqualTo(10709966848L);
        assertThat(first.getStatistics().getSeeders()).isEqualTo(15);
        assertThat(first.getStatistics().getPeers()).isEqualTo(8);
        assertThat(first.getStatistics().getDownloadedTimes()).isNull();

        var second = result.get(1);
        assertThat(second.getLink()).startsWith("magnet:?");
        assertThat(second.getStatistics().getSeeders()).isEqualTo(57);
    }

    @Test
    public void should_apply_filter_predicate() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents"))
                .willReturn(okForContentType("application/json", """
                        [
                          {"tracker": "rutracker", "title": "Too small", "size": 100, "sid": 1, "pir": 0},
                          {"tracker": "rutracker", "title": "Big enough", "size": 1073741824, "sid": 10, "pir": 1}
                        ]
                        """))
        );

        // when
        var result = sut.query("terminator", info -> info.getSize() > 1024 * 1024);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Big enough");
    }

    @Test
    public void should_return_empty_list_on_empty_result() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents"))
                .willReturn(okForContentType("application/json", "[]"))
        );

        // when
        var result = sut.query("terminator");

        // then
        assertThat(result).isEmpty();
    }

    @SneakyThrows
    @Test
    public void should_download_file_and_use_content_disposition_filename() {
        // given
        var torrentBytes = "fake-torrent-content".getBytes(StandardCharsets.UTF_8);
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents/download"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-bittorrent")
                        .withHeader("Content-Disposition", "attachment; filename=\"Terminator%202%20%5B1991%5D.torrent\"")
                        .withBody(torrentBytes))
        );
        var contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2: Judgment Day [1991]")
                .link("http://localhost:" + wm.getPort() + "/api/v1.0/torrents/download?apikey=test-api-key&id=6376416")
                .build();

        // when
        var result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_[1991].torrent");
        assertThat(result.bytes()).isEqualTo(torrentBytes);
        wm.verify(getRequestedFor(urlPathEqualTo("/api/v1.0/torrents/download"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .withQueryParam("id", equalTo("6376416")));
    }

    @SneakyThrows
    @Test
    public void should_derive_filename_from_title_when_content_disposition_missing() {
        // given
        var torrentBytes = "fake-torrent-content".getBytes(StandardCharsets.UTF_8);
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents/download"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-bittorrent")
                        .withBody(torrentBytes))
        );
        var contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2: Judgment Day [1991]")
                .link("http://localhost:" + wm.getPort() + "/api/v1.0/torrents/download?apikey=test-api-key")
                .build();

        // when
        var result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_Judgment_Day_[1991].torrent");
        assertThat(result.bytes()).isEqualTo(torrentBytes);
    }

    @Test
    public void should_download_magnet_only_entry() {
        // given
        var magnetUri = "magnet:?xt=urn:btih:def456&dn=Terminator.2";
        var contentInfo = DownloadableContentInfo.builder()
                .guid(magnetUri)
                .title("Terminator 2: Judgment Day")
                .link(magnetUri)
                .build();

        // when
        var result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_Judgment_Day.magnet");
        assertThat(result.bytes()).isEqualTo(magnetUri.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    @Test
    public void should_resolve_relative_url_against_base_url() {
        // given
        var torrentBytes = "fake-torrent-content".getBytes(StandardCharsets.UTF_8);
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents/download"))
                .withQueryParam("id", equalTo("6376416"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(torrentBytes))
        );
        var contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2: Judgment Day")
                .link("/api/v1.0/torrents/download?apikey=test-api-key&id=6376416")
                .build();

        // when
        var result = sut.download(contentInfo);

        // then
        assertThat(result.filename()).isEqualTo("Terminator_2_Judgment_Day.torrent");
        assertThat(result.bytes()).isEqualTo(torrentBytes);
    }

    @Test
    public void should_fail_when_body_is_empty() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents/download")).willReturn(ok()));
        var contentInfo = DownloadableContentInfo.builder()
                .guid("g1")
                .title("Terminator 2")
                .link("http://localhost:" + wm.getPort() + "/api/v1.0/torrents/download")
                .build();

        // when then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> sut.download(contentInfo));
    }

    @Test
    public void should_fail_on_non_2xx_search_response() {
        // given
        wm.stubFor(get(urlPathEqualTo("/api/v1.0/torrents"))
                .willReturn(serverError()));

        // when then
        assertThatThrownBy(() -> sut.query("terminator"))
                .isInstanceOf(RestClientResponseException.class);
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestClient restClient() {
            return RestClient.builder().build();
        }
    }
}
