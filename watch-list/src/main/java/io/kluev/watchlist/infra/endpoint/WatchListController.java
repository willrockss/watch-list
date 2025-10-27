package io.kluev.watchlist.infra.endpoint;

import io.kluev.watchlist.app.GetWatchListHandler;
import io.kluev.watchlist.app.PlayHandler;
import io.kluev.watchlist.app.PlayRequest;
import io.kluev.watchlist.app.PlayResponse;
import io.kluev.watchlist.app.WatchListResponse;
import io.kluev.watchlist.app.progress.ProgressHandlerV2;
import io.kluev.watchlist.app.progress.ProgressRequestV2;
import io.kluev.watchlist.app.progress.ProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@SuppressWarnings("unused")
@RestController
@Slf4j
@RequiredArgsConstructor
public class WatchListController {

    private final GetWatchListHandler getWatchListHandler;
    private final PlayHandler playHandler;
    private final ProgressHandlerV2 progressHandlerV2;

    @Deprecated
    @GetMapping("/watch-list")
    public ResponseEntity<String> index() {
        return ResponseEntity.status(HttpStatus.GONE).build();
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
    @PostMapping("/v2/progress")
    public ResponseEntity<String> progressV2(@RequestBody ProgressRequestV2 request) {
        log.trace("Received progress request v2 {}", request);
        val resp = progressHandlerV2.handle(request);
        if (resp.error() == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.badRequest().body(resp.error());
    }

    @PostMapping("/v3/progress")
    public ResponseEntity<ProgressResponse> progressV3(@RequestBody ProgressRequestV2 request) {
        log.trace("Received progress request v3 {}", request);
        val resp = progressHandlerV2.handle(request);
        if (resp.error() == null) {
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.badRequest().body(resp);
    }
}
