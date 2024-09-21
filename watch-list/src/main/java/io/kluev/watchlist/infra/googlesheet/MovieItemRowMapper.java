package io.kluev.watchlist.infra.googlesheet;

import io.kluev.watchlist.domain.MovieItem;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MovieItemRowMapper {

    private final List<Object> row;

    private @Nullable String getFullTitleOrNull() {
        // TODO Add configurable mapping
        return (String) getValueOrNull(0);
    }

    private String getExternalIdOrNull() {
        // TODO Add configurable mapping
        return (String) getValueOrNull(9);
    }

    private String getStatusOrNull() {
        // TODO Add configurable mapping
        return (String) getValueOrNull(10);
    }

    private String getFilePathOrNull() {
        // TODO Add configurable mapping
        return (String) getValueOrNull(11);
    }

    private Object getValueOrNull(int index) {
        if (index < 0) {
            return null;
        }
        return index < row.size() ? row.get(index) : null;
    }

    public MovieItem toMovieItemOrNull() {
        val fullTitle = getFullTitleOrNull();
        if (fullTitle == null) {
            log.warn("Unable to parse title from {}. Skip", row);
            return null;
        }
        val externalId = getExternalIdOrNull();
        if (externalId == null) {
            log.warn("Unable to parse externalId from {}. Skip", row);
            return null;
        }
        val status = getStatusOrNull();
        if (status == null) {
            log.warn("Unable to parse status from {}. Skip", row);
            return null;
        }
        val path = getFilePathOrNull();
        if (path == null) {
            log.warn("Unable to parse path from {}. Skip", row);
            return null;
        }
        return MovieItem.builder()
                .fullTitle(fullTitle)
                .externalId(externalId)
                .status(status)
                .filePath(path)
                .build();
    }
}
