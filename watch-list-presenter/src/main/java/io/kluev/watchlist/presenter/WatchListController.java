package io.kluev.watchlist.presenter;

import com.fasterxml.jackson.databind.type.TypeFactory;
import io.kluev.watchlist.presenter.config.WatchListBackendProperties;
import io.kluev.watchlist.presenter.dto.PlayRequest;
import io.kluev.watchlist.presenter.dto.Series;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WatchListController {
    private static final ParameterizedTypeReference<ArrayList<Series>> SERIES_LIST_TYPE_REF =
            ParameterizedTypeReference.forType(
                    TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Series.class)
            );

    private final WatchListBackendProperties backendProperties;
    private final RestClient restClient;

    @Value("${mock:false}")
    private Boolean mock;

    @GetMapping("/")
    public ModelAndView getWatchList(Map<String, Object> model) {

        if (Boolean.TRUE == mock) {
            // TODO move to NODE-RED/WireMock
            model.put("watchlist", java.util.List.of(
                    new Series("Друзья", "Друзья", "/home/user/Downloads/Friends/s02e05.mkv"),
                    new Series("Хороший_доктор", "Хороший доктор", "/home/user/Videos/The.Best.Doctor/s01e01.mkv")));
        } else {
            val resp = restClient.get()
                    .uri(backendProperties.getUrl() + "/watch-list")
                    .retrieve()
                    .body(SERIES_LIST_TYPE_REF);
            model.put("watchlist", resp);
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
