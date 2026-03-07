package io.kluev.watchlist;

import dev.restate.sdk.springboot.EnableRestate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRestate
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class WatchListApplication {

    public static void main(final String[] args) {
        SpringApplication.run(WatchListApplication.class, args);
    }

}
