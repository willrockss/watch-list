package io.kluev.watchlist.infra.kinopoisk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Film {
    @JsonProperty("filmId")
    private int filmId;

    @JsonProperty("nameRu")
    private String nameRu;

    @JsonProperty("nameEn")
    private String nameEn;

    @JsonProperty("type")
    private String type;

    @JsonProperty("year")
    private String year;

    @JsonProperty("description")
    private String description;

    @JsonProperty("filmLength")
    private String filmLength;

    @JsonProperty("countries")
    private List<Country> countries;

    @JsonProperty("genres")
    private List<Genre> genres;

    @JsonProperty("rating")
    private String rating;

    @JsonProperty("ratingVoteCount")
    private int ratingVoteCount;

    @JsonProperty("posterUrl")
    private String posterUrl;

    @JsonProperty("posterUrlPreview")
    private String posterUrlPreview;
}
