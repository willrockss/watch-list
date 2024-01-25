package io.kluev.watchlist.domain;

import lombok.*;

import java.util.Comparator;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Episode implements Comparable<Episode> {

    public static final Comparator<Episode> BY_NUMBER_COMPARATOR = Comparator.comparing(Episode::getNumber);

    private Integer number;
    private String filename;
    @Getter(AccessLevel.NONE)
    private Boolean isWatched;

    public Boolean isWatched() {
        return Objects.requireNonNullElse(isWatched, Boolean.FALSE);
    }

    @Override
    public int compareTo(@NonNull Episode episode) {
        return BY_NUMBER_COMPARATOR.compare(this, episode);
    }
}
