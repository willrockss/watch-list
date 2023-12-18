package io.kluev.watchlist.infra;

import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class NodeRedSeriesRepository implements SeriesRepository {

    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;

    private final Map<String, Series> seriesCache = new ConcurrentHashMap<>();

    @Override
    public List<Series> getInProgress() {
        val fromNodeRed = getFromNodeRed();
        return fromNodeRed.stream()
                .map(this::createFromRaw)
                .toList();
    }

    private Series createFromRaw(NodeRedWatchListResponse nodeResp) {
        val series = new Series(nodeResp.name(), generateFullTitle(nodeResp), Path.of(nodeResp.path()));
        if (Files.exists(series.getPath()) && Files.isDirectory(series.getPath())) {
            try {
                val extMap = Files
                        .list(series.getPath())
                        .filter(Files::isRegularFile)
                        .map(it -> it.getFileName().toString())
                        .map(FilenameUtils::getExtension)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                val contentExt = extMap
                        .entrySet()
                        .stream()
                        .max(Entry.comparingByValue())
                        .map(Entry::getKey)
                        .orElseThrow();

                AtomicInteger counter = new AtomicInteger(0);
                Files
                        .list(series.getPath())
                        .filter(it -> contentExt.equals(FilenameUtils.getExtension(it.getFileName().toString())))
                        .sorted()
                        .forEach(it -> series.getEpisodes().add(new Episode(
                                counter.incrementAndGet(),
                                it.getFileName().toString(),
                                counter.get() <= nodeResp.watchedEpisodeNumber(),
                                0
                        )));
            } catch (Exception e) {
                log.error("Unable to read directory: {}", series.getPath(), e);
            }
        }
        return series;
    }

    private String generateFullTitle(NodeRedWatchListResponse wlResp) {
        return "%s(%s, %d/%d)".formatted(wlResp.name(), wlResp.seasonNumber(), wlResp.watchedEpisodeNumber(), wlResp.episodesCount());
    }

    private List<NodeRedWatchListResponse> getFromNodeRed() {
        val rawResponse = restClient.get().uri(properties.getUrl() + "/watch-list").retrieve().body(
                new ParameterizedTypeReference<List<List<Object>>>() {
                });

        if (rawResponse == null) {
            return List.of();
        }

        val result = new ArrayList<NodeRedWatchListResponse>();
        boolean headerSkipped = false;

        for (List<Object> it : rawResponse) {
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }

            if (it.isEmpty()) {
                break;
            }

            val name = (String) it.get(0);
            if (StringUtils.isBlank(name)) {
                break;
            }

            val seasonNumber = parseInt(it.get(1));
            val watchedEpisodeNumber = parseInt(it.get(2));
            val episodesCount = parseInt(it.get(3));
            String path = "";
            if (it.size() >= 6) {
                path = (String) it.get(5);
            }

            result.add(new NodeRedWatchListResponse(
                    name,
                    seasonNumber,
                    watchedEpisodeNumber,
                    episodesCount,
                    path
            ));

        }

        return result;
    }

    private Integer parseInt(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Integer integerRaw) {
            return integerRaw;
        }
        if (raw instanceof Number numberRaw) {
            return numberRaw.intValue();
        }
        return Integer.parseInt(raw.toString());
    }
}

record NodeRedWatchListResponse(
        String name,
        Integer seasonNumber,
        Integer watchedEpisodeNumber,
        Integer episodesCount,
        String path
) {
}
