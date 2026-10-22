package org.xresloader.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class SharedCacheTest {
    private record Key(String name) {
        @Override public int hashCode() { return 1; }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Unrelated loads must run concurrently");
    }

    private static int pendingUsers(SharedCache<?, ?> cache, Object key) throws Exception {
        var lockField = SharedCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        var lock = (ReentrantReadWriteLock) lockField.get(cache);
        lock.readLock().lock();
        try {
            var field = SharedCache.class.getDeclaredField("pending");
            field.setAccessible(true);
            Object pending = ((Map<?, ?>) field.get(cache)).get(key);
            if (pending == null) {
                return 0;
            }
            var users = pending.getClass().getDeclaredField("users");
            users.setAccessible(true);
            return users.getInt(pending);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Test
    void distinctKeysWithTheSameHashBuildConcurrentlyAndDoNotBlockHits() throws Exception {
        var cache = new SharedCache<Key, String>();
        var warm = new Key("warm");
        cache.getOrLoad(warm, () -> "ready");
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(3);
        try {
            var jobs = new ArrayList<Future<String>>();
            for (String name : new String[] {"first", "second"}) {
                jobs.add(pool.submit(() -> cache.getOrLoad(new Key(name), () -> {
                    entered.countDown();
                    await(release);
                    return name;
                })));
            }
            await(entered);
            assertEquals("ready", pool.submit(() -> cache.getOrLoad(warm,
                    () -> { throw new AssertionError("Cache hit invoked loader"); })).get(10, TimeUnit.SECONDS));
            release.countDown();
            assertEquals("first", jobs.get(0).get(10, TimeUnit.SECONDS));
            assertEquals("second", jobs.get(1).get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void checkedFailureAndNullResultsRetryWithoutReplacingAWaitersLock() throws Exception {
        var cache = new SharedCache<String, Object>();
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var complete = new Object();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var jobs = new ArrayList<Future<Object>>();
            for (int i = 0; i < 8; ++i) {
                jobs.add(pool.submit(() -> cache.getOrLoad("shared", () -> {
                    int attempt = calls.incrementAndGet();
                    if (attempt == 1) {
                        entered.countDown();
                        await(release);
                        throw new IOException("first load failed");
                    }
                    return attempt == 2 ? null : complete;
                })));
            }
            await(entered);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (pendingUsers(cache, "shared") != 8 && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(8, pendingUsers(cache, "shared"));
            release.countDown();
            int failures = 0;
            int empty = 0;
            for (var job : jobs) {
                try {
                    Object value = job.get(10, TimeUnit.SECONDS);
                    if (value == null) {
                        ++empty;
                    } else {
                        assertSame(complete, value);
                    }
                } catch (ExecutionException exception) {
                    assertInstanceOf(IOException.class, exception.getCause());
                    ++failures;
                }
            }
            assertEquals(1, failures);
            assertEquals(1, empty);
            assertEquals(3, calls.get());
            assertEquals(0, pendingUsers(cache, "shared"));
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void nestedCacheLoadsOfDifferentKeysCanBuildTogether() throws Exception {
        var schemas = new SharedCache<String, String>();
        var inputs = new SharedCache<String, String>();
        var entered = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var jobs = new ArrayList<Future<String>>();
            for (String name : new String[] {"first", "second"}) {
                jobs.add(pool.submit(() -> schemas.getOrLoad(name, () -> inputs.getOrLoad(name, () -> {
                    entered.countDown();
                    await(entered);
                    return name;
                }))));
            }
            assertEquals("first", jobs.get(0).get(15, TimeUnit.SECONDS));
            assertEquals("second", jobs.get(1).get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void errorsAlsoReleaseLoadState() throws Exception {
        var cache = new SharedCache<String, String>();
        assertThrows(AssertionError.class,
                () -> cache.getOrLoad("key", () -> { throw new AssertionError("failed"); }));
        assertEquals(0, pendingUsers(cache, "key"));
        assertEquals("retry", cache.getOrLoad("key", () -> "retry"));
    }
}
