package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Data
@Component
@ConfigurationProperties(prefix = "download")
public class DownloadProperties {
    private DataSize minFreeDiskSpace = DataSize.ofGigabytes(50);
}