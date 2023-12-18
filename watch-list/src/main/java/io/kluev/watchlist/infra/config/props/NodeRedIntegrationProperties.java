package io.kluev.watchlist.infra.config.props;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integration.node-red")
public class NodeRedIntegrationProperties {
    @NotBlank
    private String url;
}
