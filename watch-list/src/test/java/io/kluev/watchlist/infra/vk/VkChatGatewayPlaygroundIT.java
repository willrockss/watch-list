package io.kluev.watchlist.infra.vk;

import io.kluev.watchlist.app.chat.ChatGateway;
import io.kluev.watchlist.infra.chat.ChatSessionStore;
import io.kluev.watchlist.infra.config.props.VkontakteBotProperties;
import io.kluev.watchlist.infra.vkbot.VkChatGateway;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

@SuppressWarnings("unused")
@Disabled
@EnableConfigurationProperties(VkontakteBotProperties.class)
@Tag("IntegrationTest")
@SpringBootTest(
        classes = {
                VkChatGateway.class
        }
)
@Import(VkChatGatewayPlaygroundIT.TestConfig.class)
class VkChatGatewayPlaygroundIT {

    @Autowired
    private ChatGateway chatGateway;

    @MockBean
    private ChatSessionStore chatSessionStore;

    private final ClassPathResource foundContentData = new ClassPathResource("json/found_content_with_tricky_markdown_chars.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void send_message_v2() {
        Mockito.when(chatSessionStore.findChatIdsByRawUserIds(Mockito.any())).thenReturn(List.of("3193213"));
        chatGateway.sendMessage(ChatGateway.MessageArgs.builder()
                .chatId("3193213")
                        .messageTemplate("Test %s template with %s")
                        .templateArgs(List.of("v2 send text", "SpringBootTest"))
                        .image(URI.create("https://kinopoiskapiunofficial.tech/images/posters/kp_small/444.jpg"))
                        .buttons(List.of(
                                List.of(
                                        ChatGateway.CommandButton.builder()
                                                .caption("OK Button")
                                                .action("cb_ok")
                                                .build(),
                                        ChatGateway.CommandButton.builder()
                                                .caption("Cancel Button")
                                                .action("cb_cancel")
                                                .build()
                                ),
                                List.of(ChatGateway.CommandButton.builder()
                                        .caption("Line 2 Button")
                                        .action("cb_b2")
                                        .build()),
                                List.of(ChatGateway.CommandButton.builder()
                                        .caption("Line 3 Button")
                                        .action("cb_b3")
                                        .build())
                        ))
                .build()
        );
    }

    @TestConfiguration
    public static class TestConfig {
    }

}