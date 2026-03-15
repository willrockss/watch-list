package io.kluev.watchlist.app.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CallbackCommand(
        ChatMessageResponse response,
        String command
) {
    @Override
    public String command() {
        return defaultIfNull(command, "");
    }
}
