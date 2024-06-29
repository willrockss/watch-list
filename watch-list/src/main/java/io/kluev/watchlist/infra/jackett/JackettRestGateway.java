package io.kluev.watchlist.infra.jackett;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.DownloadedContent;
import io.kluev.watchlist.infra.config.props.JackettProperties;
import io.kluev.watchlist.infra.jackett.dto.Item;
import io.kluev.watchlist.infra.jackett.dto.Rss;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class JackettRestGateway {

    private final JackettProperties properties;
    private final RestClient restClient;

    private final static ObjectMapper MAPPER = createXmlMapper();


    public List<DownloadableContentInfo> query(String query) {
        val rawResp = restClient
                .get()
                .uri(createQueryUriFunc(properties.getApiKey(), query))
                .retrieve()
                .body(String.class);
        try {
            val response = MAPPER.readValue(rawResp, Rss.class);
            return map(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse response: " + rawResp, e);
        }
    }

    public DownloadedContent download(DownloadableContentInfo contentInfo) {
        val fileResp = restClient.get().uri(contentInfo.link()).retrieve().body(Resource.class);
        if (fileResp == null) {
            throw new IllegalArgumentException("Unable to download: " + contentInfo.link());
        }
        try {
            val decodedFilename = URLDecoder.decode(requireNonNull(fileResp.getFilename()), StandardCharsets.UTF_8);
            val escapedFilename = decodedFilename.replaceAll("[^\\w.-]", "_");
            return new DownloadedContent(escapedFilename, fileResp.getContentAsByteArray());
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Unable to read downloaded file: " + contentInfo.link(), ioException);
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
                item.getTitle(),
                item.getLink(),
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
                        .build();

    }

    private static ObjectMapper createXmlMapper() {
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        return new XmlMapper(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
