package io.kluev.watchlist.infra.jackett.dto;

import lombok.Data;

@Data
public class AtomLink {
    private String href;
    private String rel;
    private String type;
}
