package io.kluev.watchlist.app;

import org.springframework.lang.Nullable;

import java.util.concurrent.locks.Lock;

public interface LockService {
    @Nullable
    Lock acquireLock(String lockId);
}
