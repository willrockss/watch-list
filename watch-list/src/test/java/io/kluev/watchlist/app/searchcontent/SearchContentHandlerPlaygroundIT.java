package io.kluev.watchlist.app.searchcontent;

import io.kluev.watchlist.app.ChatMessageResponse;
import io.kluev.watchlist.domain.MovieItem;
import io.kluev.watchlist.domain.event.MovieEnlisted;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;

import java.util.concurrent.CountDownLatch;

@Disabled
@SpringBootTest(
        properties = {
                "integration.telegram-bot.session-store-type=NOOP"
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
        val movie = MovieItem.create("Терминатор 2", "Terminator 2: Judgment Day", 1991, "123");
        val enlistedEvent = new MovieEnlisted(movie, "cranman89");
        searchContentHandler.handle(enlistedEvent);
        countDownLatch.await();
        Thread.sleep(100_000);
    }

    @EventListener(ChatMessageResponse.class)
    public void handleResponse(ChatMessageResponse rawResponse) {
        countDownLatch.countDown();
    }

}