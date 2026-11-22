package io.kluev.watchlist.infra.jackett.dto;

import tools.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RssUnmarshallingTest {

    @Test
    public void should_parse_empty_result_ok() {
        // given
        var rawResp =
                """
                        <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:torznab="http://torznab.com/schemas/2015/feed">
                          <channel>
                            <atom:link href="http://localhost:9117/" rel="self" type="application/rss+xml" />
                            <title>AggregateSearch</title>
                            <description>This feed includes all configured trackers</description>
                            <link>http://127.0.0.1/</link>
                            <language>en-US</language>
                            <category>search</category>
                          </channel>
                        </rss>
                """;

        // when
        var result = new XmlMapper().readValue(rawResp, Rss.class);

        // then
        assertThat(result.getChannel().getItem()).isEmpty();
    }

}