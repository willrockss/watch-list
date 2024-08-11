package io.kluev.watchlist.app;

import java.util.List;

public interface JackettGateway {
    List<DownloadableContentInfo> query(String query);

    FileContent download(DownloadableContentInfo contentInfo);
}
