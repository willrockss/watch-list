package io.kluev.watchlist.infra;

public record ExternalMovieDto(
        Integer year,
        String name,
        String enName,
        String externalId,
        String previewImageUrl
) {
}
