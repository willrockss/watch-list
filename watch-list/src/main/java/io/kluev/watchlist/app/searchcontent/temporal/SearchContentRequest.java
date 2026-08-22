package io.kluev.watchlist.app.searchcontent.temporal;

import io.kluev.watchlist.domain.MovieItem;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public record SearchContentRequest(
        String wfId,
        String title,
        String foreignTitle,
        Integer year,
        String externalId
) {

    public MovieItem toMovieItem() {
        if (isNotBlank(foreignTitle)) {
            return MovieItem.create(title, foreignTitle, year, externalId);
        }
        return MovieItem.create(title, year, externalId);
    }
}