package io.kluev.watchlist.app;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kluev.watchlist.infra.config.props.GoogleSheetProperties;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import io.kluev.watchlist.infra.telegrambot.TelegramSessionStore;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Since RestClientTest doesn't support RestClient with specified request factory we have to use
 * Wiremock with SpringBootTest
 */
@MockBean({
        TelegramSessionStore.class
})
@SuppressWarnings("unused")
@Tag("IntegrationTest")
@EnableConfigurationProperties({
        GoogleSheetProperties.class,
        NodeRedIntegrationProperties.class
})
@SpringBootTest (
        classes = {
                PlayHandler.class,
                RestClient.class,
        },
        properties = {
                "spring.sql.init.mode=never",
                "integration.node-red.play-video-url=/mock/play"
        }
)
@Import(PlayHandlerRestClientIntegrationTest.TestConfig.class)
class PlayHandlerRestClientIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // For some reason baseUrl return https instead of http. Construct url manually
        Supplier<Object> wmHttpHostSupplier = () -> "http://localhost:" + wm.getPort();
        registry.add("integration.node-red.url", wmHttpHostSupplier);
    }

    @Autowired
    private PlayHandler sut;


    @Test
    public void test() {
        // given
        wm.stubFor(post("/mock/play").willReturn(ok()));

        // when
        val request = new PlayRequest("testId", "testPath", VideoType.MOVIE);
        sut.handle(request);

        // then
        wm.verify(
                postRequestedFor(urlEqualTo("/mock/play"))
                        .withRequestBody(equalToJson("{\"videoId\":\"testId\",\"videoPath\":\"testPath\",\"videoType\":\"MOVIE\"}"))
        );
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            val template = new RestTemplate();
            template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            return template;
        }

        @Bean
        public RestClient restClient() {
            return RestClient
                    .builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .build();
        }
    }
}