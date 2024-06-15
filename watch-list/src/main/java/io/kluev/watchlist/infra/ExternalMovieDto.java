package io.kluev.watchlist.infra;

public record ExternalMovieDto(
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
