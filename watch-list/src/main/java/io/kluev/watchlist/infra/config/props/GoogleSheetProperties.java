package io.kluev.watchlist.infra.config.props;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "integration.google.sheet")
public class GoogleSheetProperties {

    private String spreadsheetId;

    @NotNull
    private GoogleSheetProperties.BaseSheetProperties moviesToWatch;

    @NotNull
    private GoogleSheetProperties.BaseSheetProperties watchedMovies;

    @Data
    public static class BaseSheetProperties {
        private String headerRange;
        private Map<String, String> columnsMapping;
    }

}


