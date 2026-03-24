package io.kluev.watchlist.infra.config.props;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@SuppressWarnings("unused")
@Component
@NoArgsConstructor
@ConfigurationProperties(prefix = "integration.chats.vkontakte")
public class VkontakteBotProperties {

    @NotNull
    private Boolean enabled;
    @NotEmpty
    private Long groupId;
    @NotEmpty
    private String groupToken;
    @NotEmpty
    private Set<String> admins;

}
