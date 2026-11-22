package io.kluev.watchlist.infra.jackett.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;

@JsonRootName("rss")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Rss {
    private Channel channel;
}
