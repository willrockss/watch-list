package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "search-content")
public class SearchContentProperties {
    private String torrFolder;
}
