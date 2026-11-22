package io.kluev.watchlist.app.searchcontent;

import io.kluev.watchlist.app.chat.ChatMessageResponse;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import io.kluev.watchlist.infra.googlesheet.clientimpl.GoogleSheetsClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.concurrent.CountDownLatch;

@Disabled
@Tag("IntegrationTest")
@MockitoBean(types = {GoogleSheetsClient.class, JdbcClient.class})
@SpringBootTest(
        properties = {
                "integration.telegram-bot.session-store-type=NOOP",
                "spring.temporal.connection.target=localhost:7233",
                "integration.jackett.enabled=false",
                "integration.prowlarr.enabled=true",
                "integration.prowlarr.base-url=http://localhost:9696",
                "integration.prowlarr.api-key=123",
        }
)
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
class SearchContentHandlerPlaygroundIT {

    @Autowired
    private SearchContentHandler searchContentHandler;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Test
    @SneakyThrows
    public void should_select_and_download_torr() {
        var movie = MovieItem.create("Гравитация", "Gravity", 2013, "468466");
        var enlistedEvent = new MovieEnlisted(movie, "cranman89");
        searchContentHandler.handle(enlistedEvent);
        countDownLatch.await();
        Thread.sleep(100_000);
    }

    @EventListener(ChatMessageResponse.class)
    public void handleResponse() {
        countDownLatch.countDown();
    }

}