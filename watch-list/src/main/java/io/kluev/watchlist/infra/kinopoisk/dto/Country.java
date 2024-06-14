package io.kluev.watchlist.infra.kinopoisk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Country {
    @JsonProperty("country")
    private String country;
}
