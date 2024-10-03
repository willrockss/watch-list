package io.kluev.watchlist.presenter;

import io.kluev.watchlist.presenter.config.WatchListBackendProperties;
import io.kluev.watchlist.presenter.dto.Movie;
import io.kluev.watchlist.presenter.dto.PlayRequest;
import io.kluev.watchlist.presenter.dto.Series;
import io.kluev.watchlist.presenter.dto.WatchListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WatchListController {

    public static final String MOVIE_READY_STATUS = "READY";

    private final WatchListBackendProperties backendProperties;
    private final RestClient restClient;

    @Value("${mock:false}")
    private Boolean mock;

    @GetMapping("/")
    public ModelAndView getWatchList(Map<String, Object> model) {

        if (Boolean.TRUE == mock) {
            // TODO move to NODE-RED/WireMock
            model.put("series", java.util.List.of(
                    new Series("Друзья", "Друзья", "/home/user/Downloads/Friends/s02e05.mkv"),
                    new Series("Хороший_доктор", "Хороший доктор", "/home/user/Videos/The.Best.Doctor/s01e0'1.mkv")));

            model.put("movies", java.util.List.of(
                    new Movie("123", "Терминатор", "READY", "/home/user/Videos/Terminator.mkv"),
                    new Movie("456", "Терминатор 2", "DOWNLOADING", "/home/user/Videos/Terminator_2.mkv"),
                    new Movie("789", "Брат", "READY", "/home/user/Videos/Brat.mp4")
            ));

        } else {
            val resp = restClient.get()
                    .uri(backendProperties.getUrl() + "/v2/watch-list")
                    .retrieve()
                    .body(WatchListResponse.class);
            Assert.notNull(resp, "WatchList response is null");

            model.put("series", resp.series());
            model.put("movies", resp.movies().stream().filter(it -> MOVIE_READY_STATUS.equals(it.status())).toList());
        }

        return new ModelAndView("index", model);
    }

    @PostMapping("/play")
    @ResponseBody
    public ResponseEntity<String> play(@RequestBody PlayRequest request) {
        log.info("Going to forward play request: {}", request);
        ResponseEntity<String> resp = restClient
                .post()
                .uri(backendProperties.getUrl() + "/play")
                .body(request)
                .retrieve()
                .onStatus(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return response.getStatusCode().isError();
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        log.error("Play request was not forwarded with resp code {}", response.getStatusCode());
                    }
                })
                .toEntity(String.class);

        log.info("Play request was successfully forwarded with resp code {} and body {}", resp.getStatusCode(), resp.getBody());
        return resp;
    }
}
