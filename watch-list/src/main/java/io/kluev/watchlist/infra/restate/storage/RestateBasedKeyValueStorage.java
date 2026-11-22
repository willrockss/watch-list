package io.kluev.watchlist.infra.restate.storage;


import dev.restate.client.Client;
import io.kluev.watchlist.app.KeyValueStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@ConditionalOnProperty(name = "workflow-engine", havingValue = "RESTATE")
@RequiredArgsConstructor
@Service
public class RestateBasedKeyValueStorage implements KeyValueStorage {

    private final Client restateClient;

    @Override
    public String withRandomKey(String value) {

        var key = UUID.randomUUID().toString();
        var client = KeyValueVirtualObjectClient.fromClient(restateClient, key);
        client.setValue(value);
        return key;
    }

    @Override
    public Optional<String> getValue(String key) {
        var client = KeyValueVirtualObjectClient.fromClient(restateClient, key);
        return client.getValue();
    }

}
