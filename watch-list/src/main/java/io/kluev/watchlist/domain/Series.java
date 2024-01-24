package io.kluev.watchlist.domain;

import io.kluev.watchlist.domain.event.EpisodeWatched;
import io.kluev.watchlist.domain.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Series {
    private SeriesId id;
    private String title;
    private Path path;
    private final List<Episode> episodes = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public Optional<Episode> getNextToWatchEpisode() {
        return episodes.stream().sorted().filter(it -> !it.getIsWatched()).findFirst();
    }

    public boolean markEpisodeWatched(String episodeFilename) {
        val episode = episodes
                .stream()
                .filter(it -> it.getFilename().equals(episodeFilename))
                .findFirst()
                .orElse(null);
        if (episode == null) {
            log.warn("Unable to find episode {} in {} to mark it as watched. Do nothing", episodeFilename, this);
            return false;
        }
        events.add(new EpisodeWatched(episode));
        return true;
    }
}
