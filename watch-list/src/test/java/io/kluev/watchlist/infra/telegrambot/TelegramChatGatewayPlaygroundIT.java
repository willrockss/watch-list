package io.kluev.watchlist.infra.telegrambot;

import io.kluev.watchlist.app.ChatGateway;
import io.kluev.watchlist.app.DownloadableContentInfo;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.UUID;

@Disabled
@SpringBootTest(
        properties = {
                "integration.telegram-bot.session-store-type=NOOP"
        }
)
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
class TelegramChatGatewayPlaygroundIT {

    @Autowired
    private ChatGateway chatGateway;

    @Test
    public void should_send_found_content_table() {
        val content = new ArrayList<DownloadableContentInfo>(10);
        for (int i = 0; i < 5; i++) {
            content.add(generateDownloadableContentInfo());
        }
        chatGateway.sendSelectContentRequest(UUID.randomUUID(), content);
    }

    private DownloadableContentInfo generateDownloadableContentInfo() {
        return new DownloadableContentInfo(
                "Терминатор 2: Судный день / Terminator 2: Judgment Day (Джеймс Кэмерон / James Cameron) [1991, фантастика, боевик, триллер, AC3, NTSC] [Theatrical Cut / 35mm Film Scan] Dub (ТВ-3)",
                "https://example.com",
                12123123123L,
                new DownloadableContentInfo.Statistics(100500, 100, 100)
        );
    }

}