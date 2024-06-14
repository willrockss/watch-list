package io.kluev.watchlist.infra.kinopoisk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FilmSearchResult {
    @JsonProperty("keyword")
    private String keyword;

    @JsonProperty("pagesCount")
    private int pagesCount;

    @JsonProperty("films")
    private List<Film> films;

    @JsonProperty("searchFilmsCountResult")
    private int searchFilmsCountResult;
}
