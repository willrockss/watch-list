package io.kluev.watchlist.infra.jackett.dto;

import lombok.Data;

@Data
public class Enclosure {
    private String url;
    private String length;
    private String type;
}
