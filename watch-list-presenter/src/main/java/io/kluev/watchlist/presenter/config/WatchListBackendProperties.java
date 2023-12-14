package io.kluev.watchlist.presenter.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@Component
@ConfigurationProperties(prefix = "watch-list.backend")
public class WatchListBackendProperties {

    @NotBlank
    private String url;
}
