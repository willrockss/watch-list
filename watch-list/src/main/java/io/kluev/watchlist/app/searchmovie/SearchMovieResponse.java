package io.kluev.watchlist.app.searchmovie;

import io.kluev.watchlist.infra.ExternalMovieDto;

import java.util.List;

public record SearchMovieResponse(
        List<ExternalMovieDto> foundMovies
) {
}
