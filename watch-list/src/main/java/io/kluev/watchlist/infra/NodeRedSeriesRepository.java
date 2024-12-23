package io.kluev.watchlist.infra;

import io.kluev.watchlist.app.SeriesDto;
import io.kluev.watchlist.app.SeriesRepository;
import io.kluev.watchlist.app.VideoType;
import io.kluev.watchlist.domain.Episode;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.SeriesIdGenerator;
import io.kluev.watchlist.domain.event.EpisodeWatched;
import io.kluev.watchlist.domain.event.Event;
import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import io.kluev.watchlist.infra.config.props.VideoServerProperties;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private final VideoServerProperties videoServerProperties;

    // Since it's single tenant app don't bother about memory leak here
    private final ConcurrentHashMap<String, List<Episode>> episodesCacheByPath = new ConcurrentHashMap<>();

    @Override
    public List<SeriesDto> getAllInProgress() {
        val fromNodeRed = getFromNodeRed();
        return fromNodeRed.stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Optional<Series> getInProgressById(String seriesId) {
        return getInProgress()
                .stream()
                .filter(it -> Objects.equals(it.getId().getValue(), seriesId))
                .findFirst();
    }

    private List<Series> getInProgress() {
        val fromNodeRed = getFromNodeRed();
        return fromNodeRed.stream()
                .map(this::createFromRaw)
                .toList();
    }

    @Override
    public void save(Series series) {
        // TODO Implement properly
        // Use UnitOfWork or just IdentityMap instead of seriesId casting
        val req = new HashMap<String, Object>();

        for (Event event : series.getEvents()) {
            switch (event) {
                // TODO make sure this is a last watched episode (in case of multiple EpisodeWatched events)
                case EpisodeWatched episodeWatched -> req.put("watchedEpisodeNumber", episodeWatched.episode().getNumber());
                default -> throw new IllegalStateException("Unexpected event: " + event);
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
        val series = new Series(id, generateFullTitle(nodeResp), Path.of(nodeResp.path()), nodeResp.watchedEpisodeNumber());

        val episodesFromCache = episodesCacheByPath.computeIfAbsent(series.getPath().toString(), it -> loadEpisodes(series.getPath()));
        series.getEpisodes().addAll(episodesFromCache);
        return series;
    }

    private List<Episode> loadEpisodes(Path path) {
        log.debug("Going to load episodes from {}", path);
        if (Files.exists(path) && Files.isDirectory(path)) {
            val contentExt = calculateEpisodeFileExtension(path);

            try (val episodesStream = Files.list(path)) {
                AtomicInteger counter = new AtomicInteger(0);
                return episodesStream
                        .filter(it -> contentExt.equals(FilenameUtils.getExtension(it.getFileName().toString())))
                        .sorted()
                        .map(it -> new Episode(
                                counter.incrementAndGet(),
                                it.getFileName().toString()
                        ))
                        .toList();
            } catch (Exception e) {
                log.error("Unable to read directory: {}", path, e);
            }
        }
        return List.of();
    }

    private SeriesDto map(NodeRedWatchListResponse nodeResp) {
        val serisPath = Path.of(nodeResp.path());
        val episodesFromCache = episodesCacheByPath.computeIfAbsent(nodeResp.path(), it -> loadEpisodes(serisPath));

        val toWatchEpisode = episodesFromCache.stream()
                .sorted()
                .filter(it -> it.getNumber() > nodeResp.watchedEpisodeNumber())
                .findFirst()
                .orElse(null);

        final String localPath;
        if (toWatchEpisode != null) {
            localPath = serisPath.resolve(toWatchEpisode.getFilename()).toString();
        } else {
            localPath = null;
        }

        val seriesId = seriesIdGenerator.generateId(nodeResp.name(), nodeResp.seasonNumber());
        return SeriesDto.builder()
                .id(seriesId)
                .title(generateFullTitle(nodeResp))
                .toWatchEpisodePath(localPath)
                .localPath(localPath)
                .contentStreamUrl(getContentUrlOrNull(localPath, seriesId))
                .audioTrack(nodeResp.audioTrack())
                .skipIntroOffsetSec(nodeResp.skipIntroOffsetSec())
                .build();
    }

    private @Nullable String getContentUrlOrNull(@Nullable String path, @NonNull String id) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        val requestPart = videoServerProperties.getVideoPathTemplate().formatted(
                id,
                URLEncoder.encode(path, StandardCharsets.UTF_8),
                VideoType.EPISODE.name()
        );
        return videoServerProperties.getBaseUrl() + requestPart;
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
            Integer audioTrack = null;
            if (it.size() >= 9) {
                audioTrack = parseInt(it.get(8));
            }
            Integer skipIntroOffsetSec = null;
            if (it.size() >= 10) {
                skipIntroOffsetSec = parseInt(it.get(9));
            }

            result.add(new NodeRedWatchListResponse(
                    rowNumber,
                    name,
                    seasonNumber,
                    watchedEpisodeNumber,
                    episodesCount,
                    path,
                    audioTrack,
                    skipIntroOffsetSec
            ));

        }

        return result;
    }

    private Integer parseInt(Object raw) {
        return switch (raw) {
            case null -> null;
            case Integer integerRaw -> integerRaw;
            case Number numberRaw -> numberRaw.intValue();
            default -> Integer.parseInt(raw.toString());
        };
    }
}

record NodeRedWatchListResponse(
        Integer rowNumber,
        String name,
        Integer seasonNumber,
        Integer watchedEpisodeNumber,
        Integer episodesCount,
        String path,
        Integer audioTrack,
        Integer skipIntroOffsetSec
) {
}
