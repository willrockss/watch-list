package io.kluev.watchlist.infra.jackett.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@JacksonXmlRootElement(localName = "rss")
@Data
public class Rss {
    private Channel channel;
}
