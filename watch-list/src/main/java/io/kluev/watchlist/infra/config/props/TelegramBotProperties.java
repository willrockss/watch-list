package io.kluev.watchlist.infra.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@SuppressWarnings("unused")
@Component
@ConfigurationProperties(prefix = "integration.telegram-bot")
public class TelegramBotProperties {

    SessionStoreType sessionStoreType;

    public enum SessionStoreType {
        STUB,
        PG
    }
}
