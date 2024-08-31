package io.kluev.watchlist.domain;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MovieItem {
    private String title;
    private String foreignTitle;
    private Integer year;
    @Getter
    private String externalId;

    public static MovieItem create(String title, @NotNull String nativeTitle, @NotNull Integer year, String externalId) {
        return new MovieItem(title, nativeTitle, year, externalId);
    }

    public static MovieItem create(String title, @NotNull Integer year, String externalId) {
        return new MovieItem(title, null, year, externalId);
    }

    public String getFullTitle() {
        if (foreignTitle != null) {
            return "%s (%d, %s)".formatted(title, year, foreignTitle);
        }
        return "%s (%d)".formatted(title, year);
    }
}
