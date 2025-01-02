package io.kluev.watchlist.presenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class SeriesWithIndex {
    int index;
    String id;
    String title;
    String toWatchEpisodePath;
    String contentStreamUrl;
    Integer audioTrack;
    Integer skipIntroOffsetSec;
}
