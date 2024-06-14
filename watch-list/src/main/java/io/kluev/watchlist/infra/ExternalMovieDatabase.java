package io.kluev.watchlist.infra;

import java.util.List;
import java.util.Optional;

public interface ExternalMovieDatabase {

    List<ExternalMovieDto> find(String query);

    Optional<ExternalMovieDto> getByExternalId(String externalId);
}
