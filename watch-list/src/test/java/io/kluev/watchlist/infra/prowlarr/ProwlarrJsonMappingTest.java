package io.kluev.watchlist.infra.prowlarr;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kluev.watchlist.infra.prowlarr.dto.SearchResource;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProwlarrJsonMappingTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    @Test
    public void should_map() {
        val rawJson =
        """
        [
          {
            "guid": "rutracker.org/t=6376416",
            "indexerId": 4,
            "indexer": "RuTracker.org",
            "title": "The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 720p]",
            "size": 10709966848,
            "link": "http://localhost:9696/4/download?apikey=~~api_key~~&torrent=...",
            "magnetUrl": "magnet:?xt=urn:btih:abc123&dn=The.Walking.Dead.Dead.City.S01&tr=...",
            "infoHash": "abc123",
            "seeders": 15,
            "leechers": 8,
            "protocol": "torrent",
            "downloadUrl": "http://localhost:9696/4/download?apikey=~~api_key~~&torrent=...",
            "grabs": 4397,
            "categories": [2000, 5040],
            "publishDate": "2023-07-24T08:26:11+03:00",
            "sortTitle": "walking dead dead city s01e01 6 of 6 2023 web dl 720p"
          },
          {
            "guid": "rutracker.org/t=6376580",
            "indexerId": 4,
            "indexer": "RuTracker.org",
            "title": "The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 1080p]",
            "size": 19716241408,
            "link": null,
            "magnetUrl": "magnet:?xt=urn:btih:def456&dn=The.Walking.Dead.Dead.City.S01.1080p&tr=...",
            "infoHash": "def456",
            "seeders": 57,
            "leechers": 8,
            "protocol": "magnet",
            "grabs": 7652,
            "categories": [2000, 5040],
            "publishDate": "2023-07-23T16:08:55+03:00"
          }
        ]
        """;

        val resources = List.of(mapper.readValue(rawJson, SearchResource[].class));
        assertThat(resources).hasSize(2);

        val first = resources.get(0);
        assertThat(first.getGuid()).isEqualTo("rutracker.org/t=6376416");
        assertThat(first.getTitle()).startsWith("The Walking Dead: Dead City");
        assertThat(first.getDownloadUrl()).isNotBlank();
        assertThat(first.getMagnetUrl()).startsWith("magnet:?");
        assertThat(first.getSize()).isEqualTo(10709966848L);
        assertThat(first.getSeeders()).isEqualTo(15);
        assertThat(first.getLeechers()).isEqualTo(8);
        assertThat(first.getGrabs()).isEqualTo(4397);

        val mapped = first.toDownloadableContentInfo();
        assertThat(mapped.getGuid()).isEqualTo("rutracker.org/t=6376416");
        assertThat(mapped.getTitle()).startsWith("The Walking Dead: Dead City");
        assertThat(mapped.getLink()).isEqualTo(first.getDownloadUrl());
        assertThat(mapped.getSize()).isEqualTo(10709966848L);
        assertThat(mapped.getStatistics()).isNotNull();
        assertThat(mapped.getStatistics().getDownloadedTimes()).isEqualTo(4397);
        assertThat(mapped.getStatistics().getSeeders()).isEqualTo(15);
        assertThat(mapped.getStatistics().getPeers()).isEqualTo(8);

        val second = resources.get(1).toDownloadableContentInfo();
        assertThat(second.getLink()).isEqualTo("magnet:?xt=urn:btih:def456&dn=The.Walking.Dead.Dead.City.S01.1080p&tr=...");
    }
}
