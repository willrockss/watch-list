package io.kluev.watchlist.app;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public interface JackettGateway {
    default List<DownloadableContentInfo> query(String query) {
        return query(query, Objects::nonNull);
    }

    List<DownloadableContentInfo> query(String query, @NonNull Predicate<DownloadableContentInfo> filter);

    FileContent download(DownloadableContentInfo contentInfo);
}
