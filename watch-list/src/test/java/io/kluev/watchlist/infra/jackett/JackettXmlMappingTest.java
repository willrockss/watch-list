package io.kluev.watchlist.infra.jackett;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.kluev.watchlist.infra.jackett.dto.Rss;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JackettXmlMappingTest {

    JacksonXmlModule module = new JacksonXmlModule();
    {
        module.setDefaultUseWrapper(false);
    }

    private final ObjectMapper xmlMapper = new XmlMapper(module)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @SneakyThrows
    @Test
    public void should_map() {
        val raxXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:torznab="http://torznab.com/schemas/2015/feed">
          <channel>
            <atom:link href="http://localhost:9117/" rel="self" type="application/rss+xml" />
            <title>RuTracker.org</title>
            <description>RuTracker.org is a Semi-Private Russian torrent site with a thriving file-sharing community</description>
            <link>https://rutracker.org/</link>
            <language>en-US</language>
            <category>search</category>
            <item>
              <title>The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 720p] MVO (LostFilm, HDrezka, TVShows) + Original + Sub (Rus, Eng)</title>
              <guid>https://rutracker.org/forum/viewtopic.php?t=6376416</guid>
              <jackettindexer id="rutracker">RuTracker.org</jackettindexer>
              <type>semi-private</type>
              <comments>https://rutracker.org/forum/viewtopic.php?t=6376416</comments>
              <pubDate>Mon, 24 Jul 2023 08:26:11 +0300</pubDate>
              <size>10709966848</size>
              <grabs>4397</grabs>
              <description>Ходячие мертвецы: Мертвый город / The Walking Dead: Dead City / Сезон: 1 / Серии: 1-6 из 6 (Кевин Даулинг) [2023, США, ужасы, триллер, приключения, WEB-DL 720p] MVO (LostFilm, HDrezka, TVShows) + Original + Sub (Rus, Eng)</description>
              <link>http://localhost:9117/dl/rutracker/?jackett_apikey=~~api_key~~;path=Q2ZESjhQSXdsOEZ0QVpOQ3RmcDJkV052NjVhUzVRdXl3U2IzalVOU1huczBfZEJVZW1TbFRWTkI4NVE4SzMxSUJEQU95WWhYZ0x1NVhpZmRQd2FfRW1zWlhkeUktQXZBMHk1eHdRRllCM1hROGF1WjIxT2g2aWxDZzEwVjJSSWxFbHcxNFBYeGxDUkVBTERmNkEzTnBzR0hoeVdBRDE1VmstSEM4NnRtb2p5Zy1hZ3M&amp;file=The+Walking+Dead%3A+Dead+City+%2F+S1E1-6+of+6+%5B2023%2C+WEB-DL+720p%5D+MVO+(LostFilm%2C+HDrezka%2C+TVShows)+%2B+Original+%2B+Sub+(Rus%2C+Eng)</link>
              <category>5040</category>
              <category>100266</category>
              <enclosure url="http://localhost:9117/dl/rutracker/?jackett_apikey=~~api_key~~;path=Q2ZESjhQSXdsOEZ0QVpOQ3RmcDJkV052NjVhUzVRdXl3U2IzalVOU1huczBfZEJVZW1TbFRWTkI4NVE4SzMxSUJEQU95WWhYZ0x1NVhpZmRQd2FfRW1zWlhkeUktQXZBMHk1eHdRRllCM1hROGF1WjIxT2g2aWxDZzEwVjJSSWxFbHcxNFBYeGxDUkVBTERmNkEzTnBzR0hoeVdBRDE1VmstSEM4NnRtb2p5Zy1hZ3M&amp;file=The+Walking+Dead%3A+Dead+City+%2F+S1E1-6+of+6+%5B2023%2C+WEB-DL+720p%5D+MVO+(LostFilm%2C+HDrezka%2C+TVShows)+%2B+Original+%2B+Sub+(Rus%2C+Eng)" length="10709966848" type="application/x-bittorrent" />
              <torznab:attr name="category" value="5040" />
              <torznab:attr name="category" value="100266" />
              <torznab:attr name="genre" value="" />
              <torznab:attr name="seeders" value="15" />
              <torznab:attr name="peers" value="23" />
              <torznab:attr name="minimumratio" value="1" />
              <torznab:attr name="minimumseedtime" value="0" />
              <torznab:attr name="downloadvolumefactor" value="1" />
              <torznab:attr name="uploadvolumefactor" value="1" />
            </item>
            <item>
              <title>The Walking Dead: Dead City / S1E1-6 of 6 [2023, WEB-DL 1080p] MVO (HDrezka Studio, TVShows, LostFilm) + Original + Sub (Rus, Eng)</title>
              <guid>https://rutracker.org/forum/viewtopic.php?t=6376580</guid>
              <jackettindexer id="rutracker">RuTracker.org</jackettindexer>
              <type>semi-private</type>
              <comments>https://rutracker.org/forum/viewtopic.php?t=6376580</comments>
              <pubDate>Sun, 23 Jul 2023 16:08:55 +0300</pubDate>
              <size>19716241408</size>
              <grabs>7652</grabs>
              <description>Ходячие мертвецы: Мертвый город / The Walking Dead: Dead City / Сезон: 1 / Серии: 1-6 из 6 (Кевин Даулинг, Ганджа Монтейру, Лорен С. Яконелли) [2023, США, Ужасы, триллер, приключения, WEB-DL 1080p] MVO (HDrezka Studio, TVShows, LostFilm) + Original + Sub (Rus, Eng)</description>
              <link>http://localhost:9117/dl/rutracker/?jackett_apikey=~~api_key~~;path=Q2ZESjhQSXdsOEZ0QVpOQ3RmcDJkV052NjViOTNPbk84cll6ZWRhazBuSm16LXJWa2RrWUVUQ3lDbi1BQnU1OGVZWWE2ekVqNWFaanRzazU1VnM2cGFOUkI3RnpkS0doVzdxRHpRbTViRjJnaGs3RWUxRzRURjhGWXJWTlpGSkpJVUF6RnlRS2dWUjA0UUN0MXRKR1lxd2duYnZ0cTNaWl9KWGZKUTlvbGlKWkhkOW0&amp;file=The+Walking+Dead%3A+Dead+City+%2F+S1E1-6+of+6+%5B2023%2C+WEB-DL+1080p%5D+MVO+(HDrezka+Studio%2C+TVShows%2C+LostFilm)+%2B+Original+%2B+Sub+(Rus%2C+Eng)</link>
              <category>5040</category>
              <category>100266</category>
              <enclosure url="http://localhost:9117/dl/rutracker/?jackett_apikey=~~api_key~~;path=Q2ZESjhQSXdsOEZ0QVpOQ3RmcDJkV052NjViOTNPbk84cll6ZWRhazBuSm16LXJWa2RrWUVUQ3lDbi1BQnU1OGVZWWE2ekVqNWFaanRzazU1VnM2cGFOUkI3RnpkS0doVzdxRHpRbTViRjJnaGs3RWUxRzRURjhGWXJWTlpGSkpJVUF6RnlRS2dWUjA0UUN0MXRKR1lxd2duYnZ0cTNaWl9KWGZKUTlvbGlKWkhkOW0&amp;file=The+Walking+Dead%3A+Dead+City+%2F+S1E1-6+of+6+%5B2023%2C+WEB-DL+1080p%5D+MVO+(HDrezka+Studio%2C+TVShows%2C+LostFilm)+%2B+Original+%2B+Sub+(Rus%2C+Eng)" length="19716241408" type="application/x-bittorrent" />
              <torznab:attr name="category" value="5040" />
              <torznab:attr name="category" value="100266" />
              <torznab:attr name="genre" value="" />
              <torznab:attr name="seeders" value="57" />
              <torznab:attr name="peers" value="65" />
              <torznab:attr name="minimumratio" value="1" />
              <torznab:attr name="minimumseedtime" value="0" />
              <torznab:attr name="downloadvolumefactor" value="1" />
              <torznab:attr name="uploadvolumefactor" value="1" />
            </item>
          </channel>
        </rss>
        """;

        val xml = xmlMapper.readValue(raxXml, Rss.class);
        val channel = xml.getChannel();
        assertThat(channel).isNotNull();

        val item = channel.getItem().get(1);
        assertThat(item.getTitle()).startsWith("The Walking Dead: Dead City");
        assertThat(item.getLink()).isNotBlank();
        assertThat(item.getAttr("seeders")).isEqualTo("57");
    }
}
