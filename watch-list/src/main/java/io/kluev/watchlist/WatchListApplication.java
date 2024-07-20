package io.kluev.watchlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WatchListApplication {

    public static void main(final String[] args) {
        SpringApplication.run(WatchListApplication.class, args);
    }

}
