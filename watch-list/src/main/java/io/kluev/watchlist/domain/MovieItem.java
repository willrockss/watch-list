package io.kluev.watchlist.domain;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MovieItem {
    private String title;
    private String foreignTitle;
    private Integer year;
    private String fullTitle; // TODO remove
    @Getter
    private String externalId;
    @Getter
    private String status;
    @Getter
    private String filePath;

    // TODO remove method
    public static MovieItem create(String title, @NotNull String foreignTitle, @NotNull Integer year, String externalId) {
        return MovieItem.builder()
                .title(title)
                .foreignTitle(foreignTitle)
                .year(year)
                .externalId(externalId)
                .build();
    }

    // TODO remove method
    public static MovieItem create(String title, @NotNull Integer year, String externalId) {
        return MovieItem.builder()
                .title(title)
                .year(year)
                .externalId(externalId)
                .build();
    }

    public String getFullTitle() {
        if (fullTitle == null) {
            fullTitle = calculateFullTitle();
        }
        return fullTitle;
    }

    private String calculateFullTitle() {
        if (foreignTitle != null) {
            return "%s (%d, %s)".formatted(title, year, foreignTitle);
        }
        return "%s (%d)".formatted(title, year);
    }
}
