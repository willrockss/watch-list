package io.kluev.watchlist.infra.vkbot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vk.api.sdk.events.CallbackEvent;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class MessageEvent implements CallbackEvent {

    private final JsonObject raw;

    private JsonObject object;

    public static Optional<MessageEvent> parse(JsonObject raw) {
        JsonElement type = raw.get("type");
        if (type != null
                && type.isJsonPrimitive()
                && "message_event".equals(type.getAsString())) {
            return Optional.of(new MessageEvent(raw));
        }
        return Optional.empty();
    }

    public String getEventId() {
        return getObject().get("event_id").getAsString();
    }

    public long getUserId() {
        return getObject().get("user_id").getAsLong();
    }

    public long getPeerId() {
        return getObject().get("peer_id").getAsLong();
    }

    /**
     * Unwrap JSON {"cmd": "real_command"} into "real_command
     */
    public String getPreparedCallbackData() {
        return getObject().get("payload").getAsJsonObject().get("cmd").getAsString();
    }

    private JsonObject getObject() {
        if (object == null) {
            object = raw.get("object").getAsJsonObject();
        }
        return object;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "raw=" + raw +
                '}';
    }
}
