package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "integration.qbit")
public class QBitClientProperties {
    private String url;
    /**
     * How long to wait for a magnet link metadata to be resolved before adding the torrent.
     */
    private Duration metadataTimeout = Duration.ofSeconds(60);
}