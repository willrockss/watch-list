package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "integration.google.sheet")
public class GoogleSheetProperties {
    private String spreadsheetId;
    private MoviesToWatchProperties moviesToWatch;

    @Data
    public static class MoviesToWatchProperties {
        private String headerRange;
        private Map<String, String> columnsMapping;
    }
}


