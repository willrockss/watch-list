package io.kluev.watchlist.infra;

import io.kluev.watchlist.app.LockService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * For single instance application simple ReentrantLock is enough
 */
@Slf4j
public class SimpleLockService implements LockService {
    public static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 1000;

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public Lock acquireLock(String lockId) {
        val lock = locks.computeIfAbsent(lockId, this::createNewLock);
        try {
            val isLocked = lock.tryLock(DEFAULT_LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (isLocked) {
                return lock;
            }
        } catch (InterruptedException e) {
            log.warn("Unable to acquire lock by {}. Do nothing", lockId);
        }
        locks.remove(lockId);
        return null;
    }

    private Lock createNewLock(String lockId) {
        return new LockDecorator(lockId, new ReentrantLock(), this::removeLock);
    }

    private void removeLock(String lockId) {
        locks.remove(lockId);
    }

    @RequiredArgsConstructor
    public static class LockDecorator implements Lock {

        private final String lockId;
        private final Lock delegate;
        private final @NonNull Consumer<String> onUnlock;

        @Override
        public void lock() {
            delegate.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            delegate.lockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return delegate.tryLock();
        }

        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            return delegate.tryLock(time, unit);
        }

        @Override
        public void unlock() {
            delegate.unlock();
            onUnlock.accept(lockId);
        }

        @Override
        public @NonNull Condition newCondition() {
            return delegate.newCondition();
        }
    }

}

