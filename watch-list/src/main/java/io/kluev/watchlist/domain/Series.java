package io.kluev.watchlist.domain;

import io.kluev.watchlist.domain.event.EpisodeWatched;
import io.kluev.watchlist.domain.event.Event;
import io.kluev.watchlist.domain.event.SeasonWatched;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// TODO Rename to SeriesSeason
@Getter
@Slf4j
@AllArgsConstructor
public class Series {
    private SeriesId id;
    private String title;
    private String fullTitle;
    private Path path;
    // TODO make SeasonNumber ValueObject
    private final Integer seasonNumber;
    private final List<Episode> episodes = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private int lastWatchedEpisodeNumber;

    public Optional<Episode> getNextToWatchEpisode() {
        return episodes.stream().sorted().filter(it -> it.getNumber() > lastWatchedEpisodeNumber).findFirst();
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
        if (episode.getNumber() <= lastWatchedEpisodeNumber) {
            log.debug("Episode {} is already marked as watched. Do nothing", episode);
            return false;
        }
        lastWatchedEpisodeNumber = episode.getNumber();
        events.add(new EpisodeWatched(episode));

        if (isFullyWatched()) {
            events.add(new SeasonWatched(this, seasonNumber));
        }
        return true;
    }

    public boolean isFullyWatched() {
        return lastWatchedEpisodeNumber >= episodes.size();
    }
}
