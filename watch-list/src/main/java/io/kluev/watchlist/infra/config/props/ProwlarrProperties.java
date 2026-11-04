package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "integration.prowlarr")
public class ProwlarrProperties {
    private String apiKey;
    private URI baseUrl;
}
