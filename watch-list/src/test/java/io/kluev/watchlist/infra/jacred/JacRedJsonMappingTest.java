package io.kluev.watchlist.infra.jacred;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.kluev.watchlist.infra.jacred.dto.TorrentListItem;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JacRedJsonMappingTest {

    private final ObjectMapper mapper = new JsonMapper();

    @SneakyThrows
    @Test
    public void should_map() {
        var rawJson =
        """
                [
                  {
                    "tracker": "rutracker",
                    "title": "The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 720p]",
                    "size": 10709966848,
                    "sizeName": "9.97 GiB",
                    "url": "http://localhost:9117/api/v1.0/torrents/download?apikey=~~api_key~~&id=6376416",
                    "magnet": "magnet:?xt=urn:btih:abc123&dn=The.Walking.Dead.Dead.City.S01&tr=...",
                    "sid": 15,
                    "pir": 8,
                    "createTime": "2023-07-24 08:26:11",
                    "updateTime": "2023-07-25 10:00:00",
                    "name": "The Walking Dead: Dead City",
                    "originalname": "The Walking Dead: Dead City",
                    "relased": 2023,
                    "videotype": "SERIES",
                    "quality": 3,
                    "voices": ["LostFilm", "Jaskier"],
                    "seasons": [1]
                  },
                  {
                    "tracker": "rutracker",
                    "title": "The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 1080p]",
                    "size": 19716241408,
                    "sizeName": "18.36 GiB",
                    "url": null,
                    "magnet": "magnet:?xt=urn:btih:def456&dn=The.Walking.Dead.Dead.City.S01.1080p&tr=...",
                    "sid": 57,
                    "pir": 8,
                    "quality": 4
                  }
                ]
                """;

        var items = List.of(mapper.readValue(rawJson, TorrentListItem[].class));
        assertThat(items).hasSize(2);

        var first = items.getFirst();
        assertThat(first.getTracker()).isEqualTo("rutracker");
        assertThat(first.getTitle()).startsWith("The Walking Dead: Dead City");
        assertThat(first.getSize()).isEqualTo(10709966848L);
        assertThat(first.getSizeName()).isEqualTo("9.97 GiB");
        assertThat(first.getUrl()).contains("/api/v1.0/torrents/download");
        assertThat(first.getMagnet()).startsWith("magnet:?");
        assertThat(first.getSid()).isEqualTo(15);
        assertThat(first.getPir()).isEqualTo(8);
        assertThat(first.getCreateTime()).isEqualTo("2023-07-24 08:26:11");
        assertThat(first.getUpdateTime()).isEqualTo("2023-07-25 10:00:00");
        assertThat(first.getName()).isEqualTo("The Walking Dead: Dead City");
        assertThat(first.getOriginalname()).isEqualTo("The Walking Dead: Dead City");
        assertThat(first.getRelased()).isEqualTo(2023);
        assertThat(first.getVideotype()).isEqualTo("SERIES");
        assertThat(first.getQuality()).isEqualTo(3);
        assertThat(first.getVoices()).containsExactly("LostFilm", "Jaskier");
        assertThat(first.getSeasons()).containsExactly(1);

        var mapped = first.toDownloadableContentInfo();
        // url is preferred over magnet for guid and link when present
        assertThat(mapped.getGuid()).isEqualTo(first.getUrl());
        assertThat(mapped.getLink()).isEqualTo(first.getUrl());
        assertThat(mapped.getTitle()).startsWith("The Walking Dead: Dead City");
        assertThat(mapped.getSize()).isEqualTo(10709966848L);
        assertThat(mapped.getStatistics()).isNotNull();
        assertThat(mapped.getStatistics().getDownloadedTimes()).isNull();
        assertThat(mapped.getStatistics().getSeeders()).isEqualTo(15);
        assertThat(mapped.getStatistics().getPeers()).isEqualTo(8);

        // magnet-only item falls back to magnet for guid and link
        var second = items.get(1);
        assertThat(second.getUrl()).isNull();
        var mappedSecond = second.toDownloadableContentInfo();
        assertThat(mappedSecond.getGuid()).isEqualTo(second.getMagnet());
        assertThat(mappedSecond.getLink()).isEqualTo("magnet:?xt=urn:btih:def456&dn=The.Walking.Dead.Dead.City.S01.1080p&tr=...");
        assertThat(mappedSecond.getSize()).isEqualTo(19716241408L);
        assertThat(mappedSecond.getStatistics().getSeeders()).isEqualTo(57);
        assertThat(mappedSecond.getStatistics().getPeers()).isEqualTo(8);
        assertThat(mappedSecond.getStatistics().getDownloadedTimes()).isNull();
    }
}
