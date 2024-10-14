package io.kluev.watchlist.infra;

import io.kluev.watchlist.app.LockService;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleLockServiceTest {

    private final LockService lockService = new SimpleLockService();

    @Test
    void should_not_acquire_lock_with_same_id_from_different_thread() throws InterruptedException {
        val lockRef1 = new AtomicReference<Lock>(null);
        Thread.ofVirtual().start(() -> lockRef1.set(lockService.acquireLock("test"))).join();
        assertThat(lockRef1.get()).isNotNull();
        System.out.println(lockRef1.get());

        val lockRef2 = new AtomicReference<Lock>(null);
        for (int i = 0; i < 2; i++) {
            Thread.ofVirtual().start(() -> lockRef2.set(lockService.acquireLock("test"))).join();
            assertThat(lockRef2.get()).isNull();
        }
    }
}