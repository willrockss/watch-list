package io.kluev.watchlist.app;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kluev.watchlist.infra.telegrambot.TelegramSessionStore;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Since RestClientTest doesn't support RestClient with specified request factory we have to use
 * Wiremock with SpringBootTest
 */
@SuppressWarnings("unused")
@SpringBootTest (
        properties = {
                "spring.sql.init.mode=never",
                "integration.node-red.play-video-url=/mock/play"
        }
)
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

    @MockBean
    private TelegramSessionStore telegramSessionStore;

    @Test
    public void test() {
        // given
        wm.stubFor(post("/mock/play").willReturn(ok()));

        // when
        val request = new PlayRequest("testId", "testPath");
        sut.handle(request);

        // then
        wm.verify(
                postRequestedFor(urlEqualTo("/mock/play"))
                        .withRequestBody(equalToJson("{\"videoId\":\"testId\",\"videoPath\":\"testPath\"}"))
        );
    }
}