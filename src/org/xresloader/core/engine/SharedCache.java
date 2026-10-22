package org.xresloader.core.engine;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Process-wide cache of fully constructed, read-only input data. */
public final class SharedCache<K, V> {
    @FunctionalInterface
    public interface Loader<V, E extends Exception> {
        V load() throws E;
    }

    private final HashMap<K, V> values = new HashMap<>();
    private static final class PendingLoad {
        final ReentrantLock lock = new ReentrantLock();
        // Includes waiters. Protected by the global index lock, not the per-key lock.
        int users;
    }

    private final HashMap<K, PendingLoad> pending = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private V find(K key) {
        lock.readLock().lock();
        try {
            return values.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Loaders may load other keys/caches, but must not form cyclic cache dependencies. */
    public <E extends Exception> V getOrLoad(K key, Loader<V, E> loader) throws E {
        V cached = find(key);
        if (cached != null) {
            return cached;
        }

        PendingLoad loading;
        lock.writeLock().lock();
        try {
            V value = values.get(key);
            if (value != null) {
                return value;
            }
            loading = pending.computeIfAbsent(key, ignored -> new PendingLoad());
            ++loading.users;
        } finally {
            lock.writeLock().unlock();
        }
        try {
            // Do not hold the index lock while waiting or executing potentially slow/nested loaders.
            loading.lock.lock();
            try {
                V value = find(key);
                if (value != null) {
                    return value;
                }
                value = loader.load();
                if (value != null) {
                    lock.writeLock().lock();
                    try {
                        values.put(key, value);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                return value;
            } finally {
                loading.lock.unlock();
            }
        } finally {
            lock.writeLock().lock();
            try {
                if (--loading.users == 0) {
                    pending.remove(key);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
