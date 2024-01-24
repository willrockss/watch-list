package io.kluev.watchlist.domain;


import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

public class SeriesIdGenerator {

    public String generateId(String title, Integer seasonNumber) {
        Assert.isTrue(StringUtils.isNotBlank(title), "title must not be blank");
        Assert.notNull(seasonNumber, "seasonNumber must not be null");
        return title
                .toLowerCase()
                .replaceAll("[^a-z0-9а-я]", "_")
                + "_" + seasonNumber;
    }
}
