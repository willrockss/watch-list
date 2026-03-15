package io.kluev.watchlist.infra.restate.storage;

import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.common.StateKey;
import dev.restate.sdk.springboot.RestateVirtualObject;

import java.util.Optional;

@RestateVirtualObject
public class KeyValueVirtualObject {

    private static final StateKey<String> VALUE_KEY = StateKey.of("value", String.class);

    @Handler
    public void setValue(ObjectContext ctx, String value) {
        ctx.set(VALUE_KEY, value);
    }

    @Handler
    public Optional<String> getValue(ObjectContext ctx) {
        return ctx.get(VALUE_KEY);
    }
}
