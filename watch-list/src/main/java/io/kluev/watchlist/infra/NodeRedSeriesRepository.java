package io.kluev.watchlist.infra;

import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.SeriesRepository;
import io.kluev.watchlist.domain.event.EpisodeWatched;
import io.kluev.watchlist.domain.event.Event;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class NodeRedSeriesRepository implements SeriesRepository {
    private final static ParameterizedTypeReference<List<List<Object>>> RESPONSE_TYPE = new ParameterizedTypeReference<>(){};

    private final SeriesIdGenerator seriesIdGenerator;
    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;

    @Override
    public List<Series> getInProgress() {
        val fromNodeRed = getFromNodeRed();
        return fromNodeRed.stream()
                .map(this::createFromRaw)
                .toList();
    }

    @Override
    public Optional<Series> getInProgressById(String seriesId) {
        return getInProgress()
                .stream()
                .filter(it -> Objects.equals(it.getId().getValue(), seriesId))
                .findFirst();
    }

    @Override
    public void save(Series series) {
        // TODO Implement properly
        // Use UnitOfWork or just IdentityMap instead of seriesId casting
        val req = new HashMap<String, Object>();

        for (Event event : series.getEvents()) {
            switch (event) {
                case EpisodeWatched episodeWatched -> req.put("watchedEpisodeNumber", episodeWatched.episode().getNumber());
                default -> throw new IllegalStateException("Unexpected value: " + event);
            }
        }

        if (req.isEmpty()) {
            log.debug("Nothing to update for {}", series);
            return;
        }

        if (series.getId() instanceof NodeRedSeriesId id) {
            val rawResponse = restClient
                    .patch()
                    .uri(properties.getUrl() + "/watch-list/" + id.getSheetRowNumber())
                    .body(req)
                    .retrieve()
                    .body(Map.class);

            log.debug("Update response {}", rawResponse);
        } else {
            throw new IllegalStateException("Unexpected id value: " + series.getId());
        }

    }

    private Series createFromRaw(NodeRedWatchListResponse nodeResp) {
        val id = new NodeRedSeriesId(seriesIdGenerator.generateId(nodeResp.name(), nodeResp.seasonNumber()), nodeResp.rowNumber());
        val series = new Series(id, generateFullTitle(nodeResp), Path.of(nodeResp.path()));
        if (Files.exists(series.getPath()) && Files.isDirectory(series.getPath())) {
            val contentExt = calculateEpisodeFileExtension(series.getPath());

            try (val episodesStream = Files.list(series.getPath())) {
                AtomicInteger counter = new AtomicInteger(0);
                episodesStream
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

    private String calculateEpisodeFileExtension(Path seriesPath) {
        try (val episodesStream = Files.list(seriesPath)) {
            val extensionFrequencyMap = episodesStream
                    .filter(Files::isRegularFile)
                    .map(it -> it.getFileName().toString())
                    .map(FilenameUtils::getExtension)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            return extensionFrequencyMap
                    .entrySet()
                    .stream()
                    .max(Entry.comparingByValue())
                    .map(Entry::getKey)
                    .orElseThrow();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read directory: " + seriesPath, e);
        }
    }

    private String generateFullTitle(NodeRedWatchListResponse wlResp) {
        return "%s(%s, %d/%d)".formatted(wlResp.name(), wlResp.seasonNumber(), wlResp.watchedEpisodeNumber(), wlResp.episodesCount());
    }

    private List<NodeRedWatchListResponse> getFromNodeRed() {
        val rawResponse = restClient
                .get()
                .uri(properties.getUrl() + "/watch-list")
                .retrieve()
                .body(RESPONSE_TYPE);

        if (rawResponse == null) {
            return List.of();
        }

        val result = new ArrayList<NodeRedWatchListResponse>();
        boolean headerSkipped = false;
        int rowNumber = 0;

        for (List<Object> it : rawResponse) {
            rowNumber++;
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
                    rowNumber,
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
        Integer rowNumber,
        String name,
        Integer seasonNumber,
        Integer watchedEpisodeNumber,
        Integer episodesCount,
        String path
) {
}
