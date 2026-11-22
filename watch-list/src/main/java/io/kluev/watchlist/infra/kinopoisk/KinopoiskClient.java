package io.kluev.watchlist.infra.kinopoisk;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.kluev.watchlist.app.ExternalMovieDatabase;
import io.kluev.watchlist.infra.kinopoisk.dto.Film;
import io.kluev.watchlist.infra.kinopoisk.dto.FilmSearchResult;
import io.kluev.watchlist.infra.kinopoisk.dto.GetFilmResult;
import lombok.SneakyThrows;

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
import java.util.Set;

public class KinopoiskClient implements ExternalMovieDatabase {

    public static final Set<String> MOVIES_TYPES = Set.of("MOVIE", "VIDEO", "FILM");

    private final String apiKey;
    private final HttpClient httpClient;

    private final ObjectMapper objectMapper = new JsonMapper();

    // TODO limit cache or use Spring Cache
    private final Map<String, ExternalMovieDto> cache = new HashMap<>();

    public KinopoiskClient(String apiKey) {
        this.apiKey = apiKey;
        httpClient = HttpClient.newHttpClient();
    }

    public List<Film> findRaw(String query) throws Exception {
        // For some reason in Uppers case results are more correct. Try search "изгой" vs "ИЗГОЙ"
        var encodedQuery = URLEncoder.encode(query.toUpperCase(), Charset.defaultCharset());
        var uri = new URI("https://kinopoiskapiunofficial.tech/api/v2.1/films/search-by-keyword?page=1&keyword=" + encodedQuery);
        var req = HttpRequest
                .newBuilder(uri)
                .GET()
                .setHeader("X-API-KEY", apiKey)
                .setHeader("Content-Type", "application/json")
                .build();
        String respBody = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
        var films = objectMapper.readValue(respBody, FilmSearchResult.class).getFilms();
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
                .filter(it -> MOVIES_TYPES.contains(it.getType()))
                .map(this::mapDto)
                .peek(it -> cache.put(it.externalId(), it))
                .toList();
    }

    @SneakyThrows
    @Override
    public Optional<ExternalMovieDto> getByExternalId(String externalId) {
        var cachedValue = cache.get(externalId);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }
        var req = HttpRequest.newBuilder(
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
                parseIntegerOrNull(film.getYear()),
                film.getNameRu(),
                film.getNameEn(),
                String.valueOf(film.getFilmId()),
                film.getPosterUrlPreview()
        );
    }

    private Integer parseIntegerOrNull(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
