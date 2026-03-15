package io.kluev.watchlist.infra.restate.storage;


import dev.restate.client.Client;
import io.kluev.watchlist.app.KeyValueStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RestateBasedKeyValueStorage implements KeyValueStorage {

    private final Client restateClient;

    @Override
    public String withRandomKey(String value) {

        val key = UUID.randomUUID().toString();
        val client = KeyValueVirtualObjectClient.fromClient(restateClient, key);
        client.setValue(value);
        return key;
    }

    @Override
    public Optional<String> getValue(String key) {
        val client = KeyValueVirtualObjectClient.fromClient(restateClient, key);
        return client.getValue();
    }

}
