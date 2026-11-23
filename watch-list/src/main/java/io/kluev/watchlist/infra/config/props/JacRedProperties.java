package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "integration.jacred")
public class JacRedProperties {
    private String apiKey;
    private URI baseUrl;
    private String sort = "pir";
    private List<String> trackers = List.of("rutracker", "korsars", "nnmclub", "bitru");
}
