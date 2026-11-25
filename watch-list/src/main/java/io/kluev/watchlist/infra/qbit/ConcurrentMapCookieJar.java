package io.kluev.watchlist.infra.qbit;

import lombok.NonNull;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ConcurrentMapCookieJar implements CookieJar {
    private final ConcurrentMap<String, List<Cookie>> store = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        if (!cookies.isEmpty()) {
            store.put(url.host(), cookies);
        }
    }

    @Override
    public @NonNull List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        return store.getOrDefault(url.host(), List.of());
    }
}
