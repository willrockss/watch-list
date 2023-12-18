package io.kluev.watchlist.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Series {
    private String id;
    private String title;
    private Path path;
    private final List<Episode> episodes = new ArrayList<>();

    public Optional<Episode> getNextToWatchEpisode() {
        return episodes.stream().sorted().filter(it -> !it.getIsWatched()).findFirst();
    }
}
