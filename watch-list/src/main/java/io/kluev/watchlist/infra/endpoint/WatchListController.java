package io.kluev.watchlist.infra.endpoint;

import io.kluev.watchlist.app.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@Slf4j
@RequiredArgsConstructor
public class WatchListController {

    private final GetWatchListHandler getWatchListHandler;
    private final PlayHandler playHandler;
    private final ProgressHandlerV2 progressHandlerV2;


    @Deprecated
    @GetMapping("/watch-list")
    public List<SeriesDto> index() {
        return getWatchListHandler.handle().series();
    }

    @GetMapping("/v2/watch-list")
    public WatchListResponse watchList() {
        return getWatchListHandler.handle();
    }

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody PlayRequest request) {
        PlayResponse resp = playHandler.handle(request);
        if (resp.error() == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.badRequest().body(resp.error());
    }

    @Deprecated
    @PostMapping("/progress")
    public ResponseEntity<String> progress(@RequestBody Object request) {
        log.trace("Received progress request {}", request);
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @PostMapping("/v2/progress")
    public ResponseEntity<String> progress(@RequestBody ProgressRequestV2 request) {
        log.trace("Received progress request v2 {}", request);
        val resp = progressHandlerV2.handle(request);
        if (resp.error() == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.badRequest().body(resp.error());
    }
}
