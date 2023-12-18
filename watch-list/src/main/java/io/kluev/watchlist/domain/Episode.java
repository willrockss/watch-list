package io.kluev.watchlist.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Episode implements Comparable<Episode> {

    public static final Comparator<Episode> BY_NUMBER_COMPARATOR = Comparator.comparing(Episode::getNumber);

    private Integer number;
    private String filename;
    private Boolean isWatched;
    private Integer progressPermille = 0;

    @Override
    public int compareTo(Episode episode) {
        return BY_NUMBER_COMPARATOR.compare(this, episode);
    }
}
