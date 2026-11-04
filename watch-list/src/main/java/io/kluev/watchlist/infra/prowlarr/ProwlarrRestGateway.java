package io.kluev.watchlist.infra.prowlarr;

import io.kluev.watchlist.app.DownloadableContentGateway;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.FileContent;
import io.kluev.watchlist.infra.config.props.ProwlarrProperties;
import io.kluev.watchlist.infra.jackett.WLFilenameUtils;
import io.kluev.watchlist.infra.prowlarr.dto.SearchResource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
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

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class ProwlarrRestGateway implements DownloadableContentGateway {

    public static final int DEFAULT_PAGE_SIZE = 40;
    public static final String DEFAULT_CATEGORIES = "2000,5000";

    private final ProwlarrProperties properties;
    private final RestClient restClient;

    @Override
    public List<DownloadableContentInfo> query(@NonNull String query, @NonNull Predicate<DownloadableContentInfo> filter) {
        Assert.notNull(query, "Query must not be null!");

        val rawResp = restClient
                .get()
                .uri(createQueryUriFunc(properties.getApiKey(), query))
                .retrieve()
                .body(SearchResource[].class);
        if (rawResp == null) {
            throw new IllegalArgumentException("Unable to fetch search results for query: " + query);
        }
        return Arrays.stream(rawResp)
                .map(SearchResource::toDownloadableContentInfo)
                .filter(filter)
                .toList();
    }

    @Override
    public FileContent download(@NonNull DownloadableContentInfo contentInfo) {
        val fileResp = restClient.get().uri(contentInfo.getLink()).retrieve().body(Resource.class);
        if (fileResp == null) {
            throw new IllegalArgumentException("Unable to download: " + contentInfo.getLink());
        }
        try {
            val filename = fileResp.getFilename() != null
                    ? URLDecoder.decode(fileResp.getFilename(), StandardCharsets.UTF_8)
                    : deriveFilename(contentInfo);
            val escapedFilename = WLFilenameUtils.escapeFilename(filename);
            return new FileContent(escapedFilename, fileResp.getContentAsByteArray());
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Unable to read downloaded file: " + contentInfo.getLink(), ioException);
        }
    }

    private String deriveFilename(DownloadableContentInfo contentInfo) {
        val link = requireNonNull(contentInfo.getLink());
        val extension = link.startsWith("magnet:") ? ".magnet" : ".torrent";
        return contentInfo.getTitle() + extension;
    }

    private Function<UriBuilder, URI> createQueryUriFunc(String apiKey, String query) {
        return (UriBuilder uriBuilder) -> {
            val uri = uriBuilder
                    .scheme(properties.getBaseUrl().getScheme())
                    .host(properties.getBaseUrl().getHost())
                    .port(properties.getBaseUrl().getPort())
                    .path("/api/v1/search")
                    .queryParam("apikey", apiKey)
                    .queryParam("query", query)
                    .queryParam("limit", DEFAULT_PAGE_SIZE)
                    .queryParam("type", "search");
            for (val category : DEFAULT_CATEGORIES.split(",")) {
                uri.queryParam("categories", category.trim());
            }
            return uri.build();
        };
    }

}
