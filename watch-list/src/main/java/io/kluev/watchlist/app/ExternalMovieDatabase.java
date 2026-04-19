package io.kluev.watchlist.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

public interface ExternalMovieDatabase {

    List<ExternalMovieDto> find(String query);

    Optional<ExternalMovieDto> getByExternalId(String externalId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExternalMovieDto(
            Integer year,
            String name,
            String enName,
            String externalId,
            String previewImageUrl
    ) {

        public String getFullName() {
            if (enName != null) {
                return "%s (%d, %s)".formatted(name, year, enName);
            }
            return "%s (%d)".formatted(name, year);
        }
    }
}
