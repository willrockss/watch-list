package io.kluev.watchlist.infra.endpoint;

import io.kluev.watchlist.app.*;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class WatchListController {

    private final GetWatchListHandler getWatchListHandler;
    private final PlayHandler playHandler;


    @GetMapping("/watch-list")
    public List<SeriesDto> index() {
        return getWatchListHandler.handle().seriesDtos();
    }

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody PlayRequest request) {
        PlayResponse resp = playHandler.handle(request);
        if (resp.error() == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.badRequest().body(resp.error());
    }

}
