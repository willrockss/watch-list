package io.kluev.watchlist.infra.kinopoisk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Genre {
    @JsonProperty("genre")
    private String genre;
}
