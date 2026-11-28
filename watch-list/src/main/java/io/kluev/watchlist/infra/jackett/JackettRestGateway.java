package io.kluev.watchlist.infra.jackett;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.FileContent;
import io.kluev.watchlist.app.DownloadableContentGateway;
import io.kluev.watchlist.infra.config.props.JackettProperties;
import io.kluev.watchlist.infra.jackett.dto.Item;
import io.kluev.watchlist.infra.jackett.dto.Rss;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class JackettRestGateway implements DownloadableContentGateway {

    public static final int DEFAULT_PAGE_SIZE = 40;
    private final static ObjectMapper MAPPER = createXmlMapper();

    private final JackettProperties properties;
    private final RestClient restClient;

    @Override
    public List<DownloadableContentInfo> query(@NonNull String query, @NonNull Predicate<DownloadableContentInfo> filter) {
        Assert.notNull(query, "Query must not be null!");

        var rawResp = restClient
                .get()
                .uri(createQueryUriFunc(properties.getApiKey(), query.toLowerCase(Locale.ROOT)))
                .retrieve()
                .body(String.class);
        try {
            var response = MAPPER.readValue(rawResp, Rss.class);
            return map(response)
                    .stream()
                    .filter(filter)
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse response: " + rawResp, e);
        }
    }

    @Override
    public FileContent download(@NonNull DownloadableContentInfo contentInfo) {
        var fileResp = restClient.get().uri(contentInfo.getLink()).retrieve().body(Resource.class);
        if (fileResp == null) {
            throw new IllegalArgumentException("Unable to download: " + contentInfo.getLink());
        }
        try {
            var decodedFilename = URLDecoder.decode(requireNonNull(fileResp.getFilename()), StandardCharsets.UTF_8);
            var escapedFilename = WLFilenameUtils.escapeFilename(decodedFilename);
            return new FileContent(escapedFilename, fileResp.getContentAsByteArray());
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Unable to read downloaded file: " + contentInfo.getLink(), ioException);
        }
    }

    private List<DownloadableContentInfo> map(Rss rss) {
        return rss.getChannel()
                .getItem()
                .stream()
                .map(this::map)
                .toList();
    }

    private DownloadableContentInfo map(Item item) {
        return new DownloadableContentInfo(
                item.getGuid(),
                item.getTitle(),
                item.getLink(),
                null, // no magnet uri in Jackett rss feed
                item.getSize(),
                new DownloadableContentInfo.Statistics(
                        item.getGrabs(),
                        Integer.valueOf(item.getAttr("seeders")),
                        Integer.valueOf(item.getAttr("peers"))
                )
        );
    }

    private Function<UriBuilder, URI> createQueryUriFunc(String apiKey, String query) {
        return (UriBuilder uriBuilder) ->
                uriBuilder
                        .scheme(properties.getBaseUrl().getScheme())
                        .host(properties.getBaseUrl().getHost())
                        .port(properties.getBaseUrl().getPort())
                        .path("/api/v2.0/indexers/all/results/torznab/api")
                        .queryParam("apikey", apiKey)
                        .queryParam("t", "search")
                        .queryParam("q", query)
                        .queryParam("limit", DEFAULT_PAGE_SIZE)
                        .queryParam("sort", "size")
                        .build();

    }

    private static ObjectMapper createXmlMapper() {
        return XmlMapper.builder()
                .defaultUseWrapper(false)
                .build();
    }
}
