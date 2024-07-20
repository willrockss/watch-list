package io.kluev.watchlist.infra.config.props;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@SuppressWarnings("unused")
@Component
@ConfigurationProperties(prefix = "integration.telegram-bot")
public class TelegramBotProperties {

    private SessionStoreType sessionStoreType;

    @NotEmpty
    private Set<String> admins;

    public enum SessionStoreType {
        NOOP,
        PG
    }
}
