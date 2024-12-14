package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.TelegramBotProperties;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Disabled
@EnableConfigurationProperties(TelegramBotProperties.class)
@Tag("IntegrationTest")
@SpringBootTest(
        classes = {
                TelegramChatGateway.class
        }
)
@Import(TelegramChatGatewayPlaygroundIT.TestConfig.class)
class TelegramChatGatewayPlaygroundIT {

    @Autowired
    private ChatGateway chatGateway;

    @MockBean
    private TelegramSessionStore telegramSessionStore;

    private final ClassPathResource foundContentData = new ClassPathResource("json/found_content_with_tricky_markdown_chars.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void should_send_found_content_table() {
        Mockito.when(telegramSessionStore.findChatIdsByUsernames(Mockito.any())).thenReturn(List.of("521320812"));

        val content = new ArrayList<DownloadableContentInfo>(10);
        for (int i = 0; i < 5; i++) {
            content.add(generateDownloadableContentInfo());
        }
        chatGateway.sendSelectContentRequest(UUID.randomUUID(), content);
    }


    @Test
    public void should_send_markdown_result() throws IOException {
        Mockito.when(telegramSessionStore.findChatIdsByUsernames(Mockito.any())).thenReturn(List.of("521320812"));
        TypeReference<List<DownloadableContentInfo>> myType = new TypeReference<>() {};
        try(val is = foundContentData.getInputStream()) {
            List<DownloadableContentInfo> content = objectMapper.readValue(is.readAllBytes(), myType);
            chatGateway.sendSelectContentRequest(UUID.randomUUID(), content);
        }
    }

    private DownloadableContentInfo generateDownloadableContentInfo() {
        return new DownloadableContentInfo(
                "123",
                "Терминатор 2: Судный день / Terminator 2: Judgment Day (Джеймс Кэмерон / James Cameron) [1991, фантастика, боевик, триллер, AC3, NTSC] [Theatrical Cut / 35mm Film Scan] Dub (ТВ-3)",
                "https://example.com",
                12123123123L,
                new DownloadableContentInfo.Statistics(100500, 100, 100)
        );
    }

    @Test
    public void send_message() {
        Mockito.when(telegramSessionStore.findChatIdsByUsernames(Mockito.any())).thenReturn(List.of("521320812"));
        chatGateway.sendMessage("521320812", "`File: %s` is ready", "[{some file with chars to escape}].txt");
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public TelegramClient okHttpTelegramClient(@Value("${integration.telegram-bot.api.key}") String apiKey) {
            return new OkHttpTelegramClient(apiKey);
        }
    }

}