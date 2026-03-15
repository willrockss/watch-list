package io.kluev.watchlist.app;


import java.util.Optional;

public interface KeyValueStorage {
     String withRandomKey(String value);
     Optional<String> getValue(String key);
}
