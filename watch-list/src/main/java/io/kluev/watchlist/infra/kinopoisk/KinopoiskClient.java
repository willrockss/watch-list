package io.kluev.watchlist.infra.kinopoisk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kluev.watchlist.infra.ExternalMovieDatabase;
import io.kluev.watchlist.infra.ExternalMovieDto;
import io.kluev.watchlist.infra.kinopoisk.dto.Film;
import io.kluev.watchlist.infra.kinopoisk.dto.FilmSearchResult;
import io.kluev.watchlist.infra.kinopoisk.dto.GetFilmResult;
import lombok.SneakyThrows;
import lombok.val;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KinopoiskClient implements ExternalMovieDatabase {
    private final String apiKey;
    private final HttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // TODO limit cache or use Spring Cache
    private final Map<String, ExternalMovieDto> cache = new HashMap<>();

    public KinopoiskClient(String apiKey) {
        this.apiKey = apiKey;
        httpClient = HttpClient.newHttpClient();
    }

    public List<Film> findRaw(String query) throws Exception {
        val encodedQuery = URLEncoder.encode(query, Charset.defaultCharset());
        val uri = new URI("https://kinopoiskapiunofficial.tech/api/v2.1/films/search-by-keyword?page=1&keyword=" + encodedQuery);
        val req = HttpRequest
                .newBuilder(uri)
                .GET()
                .setHeader("X-API-KEY", apiKey)
                .setHeader("Content-Type", "application/json")
                .build();
        String respBody = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
        val films = objectMapper.readValue(respBody, FilmSearchResult.class).getFilms();
        if (films == null) {
            return List.of();
        }
        return films;
    }

    @SneakyThrows
    @Override
    public List<ExternalMovieDto> find(String query) {
        return findRaw(query)
                .stream()
                .filter(it -> it.getType().equals("FILM"))
                .map(this::mapDto)
                .peek(it -> cache.put(it.externalId(), it))
                .toList();
    }

    @SneakyThrows
    @Override
    public Optional<ExternalMovieDto> getByExternalId(String externalId) {
        val cachedValue = cache.get(externalId);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }
        val req = HttpRequest.newBuilder(
            new URI("https://kinopoiskapiunofficial.tech/api/v2.1/films/" + externalId))
                .GET()
                .setHeader("X-API-KEY", apiKey)
                .setHeader("Content-Type", "application/json")
                .build();
        String respBody = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
        Optional<ExternalMovieDto> response;
        response = Optional.ofNullable(objectMapper.readValue(respBody, GetFilmResult.class))
                .map(GetFilmResult::getFilm)
                .map(this::mapDto);

        // Ugly Java Optional API
        response.ifPresent(it -> cache.put(it.externalId(), it));
        return response;
    }

    private ExternalMovieDto mapDto(Film film) {
        return new ExternalMovieDto(
                Integer.valueOf(film.getYear()),
                film.getNameRu(),
                film.getNameEn(),
                String.valueOf(film.getFilmId()),
                film.getPosterUrlPreview()
        );
    }
}
