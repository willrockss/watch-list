package io.kluev.watchlist.infra.jacred;

import io.kluev.watchlist.app.DownloadableContentGateway;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.FileContent;
import io.kluev.watchlist.infra.config.props.JacRedProperties;
import io.kluev.watchlist.infra.jacred.dto.TorrentListItem;
import io.kluev.watchlist.infra.jackett.WLFilenameUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class JacRedRestGateway implements DownloadableContentGateway {

    private final JacRedProperties properties;
    private final RestClient restClient;

    @Override
    public List<DownloadableContentInfo> query(@NonNull String query, @NonNull Predicate<DownloadableContentInfo> filter) {
        Assert.notNull(query, "Query must not be null!");

        var rawResp = restClient
                .get()
                .uri(createQueryUriFunc(properties.getApiKey(), query))
                .retrieve()
                .body(TorrentListItem[].class);
        if (rawResp == null) {
            throw new IllegalArgumentException("Unable to fetch search results for query: " + query);
        }
        return Arrays.stream(rawResp)
                .map(TorrentListItem::toDownloadableContentInfo)
                .filter(filter)
                .toList();
    }

    @Override
    public FileContent download(@NonNull DownloadableContentInfo contentInfo) {
        var link = contentInfo.getLink();
        if (link != null && !link.startsWith("magnet:")) {
            return downloadTorrentFile(link, contentInfo);
        }
        var magnet = link != null ? link : contentInfo.getMagnet();
        if (magnet != null) {
            var magnetFilename = WLFilenameUtils.escapeFilename(contentInfo.getTitle() + ".magnet");
            return new FileContent(magnetFilename, magnet.getBytes(StandardCharsets.UTF_8));
        }
        throw new IllegalArgumentException("Neither torrent url nor magnet link is available for: " + contentInfo.getTitle());
    }

    private FileContent downloadTorrentFile(@NonNull String link, @NonNull DownloadableContentInfo contentInfo) {
        var fileResp = restClient.get().uri(resolveLink(link)).retrieve().body(Resource.class);
        if (fileResp == null) {
            throw new IllegalArgumentException("Unable to download: " + link);
        }
        try {
            var filename = fileResp.getFilename() != null
                    ? URLDecoder.decode(fileResp.getFilename(), StandardCharsets.UTF_8)
                    : contentInfo.getTitle() + ".torrent";
            var escapedFilename = WLFilenameUtils.escapeFilename(filename);
            return new FileContent(escapedFilename, fileResp.getContentAsByteArray());
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Unable to read downloaded file: " + link, ioException);
        }
    }

    private URI resolveLink(String link) {
        if (link.startsWith("/")) {
            return properties.getBaseUrl().resolve(link);
        }
        return URI.create(link);
    }

    private Function<UriBuilder, URI> createQueryUriFunc(String apiKey, String query) {
        return (UriBuilder uriBuilder) -> {
            var uri = uriBuilder
                    .scheme(properties.getBaseUrl().getScheme())
                    .host(properties.getBaseUrl().getHost())
                    .port(properties.getBaseUrl().getPort())
                    .path("/api/v1.0/torrents")
                    .queryParam("apikey", apiKey)
                    .queryParam("search", query)
                    .queryParam("sort", properties.getSort());
            var trackers = properties.getTrackers();
            if (trackers != null && !trackers.isEmpty()) {
                uri.queryParam("tracker", String.join(",", trackers));
            }
            return uri.build();
        };
    }
}
