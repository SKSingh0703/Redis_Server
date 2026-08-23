package Components;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store is the in-memory database component managing key-value pairs.
 * 
 * Thread-Safety: Uses ConcurrentHashMap to allow lock-safe concurrent reads and writes
 * across worker threads processing client sockets.
 * 
 * Key Eviction Strategy: Implements Passive Eviction (Lazy Eviction). When a key is requested via
 * get(), if the key is found but has expired, it is lazily removed from the map and null is returned.
 */
@Component
public class Store {

    private final ConcurrentHashMap<String, Value> map;

    public Store() {
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * Stores a key-value pair without expiration (persistent).
     */
    public void set(String key, String value) {
        map.put(key, new Value(value));
    }

    /**
     * Stores a key-value pair with a Time-To-Live (TTL) in milliseconds.
     */
    public void set(String key, String value, Long ttlMillis) {
        if (ttlMillis == null || ttlMillis <= 0) {
            set(key, value);
        } else {
            long expiryTimestamp = System.currentTimeMillis() + ttlMillis;
            map.put(key, new Value(value, expiryTimestamp));
        }
    }

    /**
     * Retrieves a Value by key.
     * Performs passive (lazy) expiration check: if key is expired, it is evicted on read and returns null.
     */
    public Value get(String key) {
        Value val = map.get(key);
        if (val == null) {
            return null;
        }

        // Passive Expiration Check
        if (val.isExpired()) {
            map.remove(key); // Evict expired key lazily
            return null;
        }

        return val;
    }

    /**
     * Deletes a key from the store.
     */
    public boolean delete(String key) {
        return map.remove(key) != null;
    }

    /**
     * Returns all active key names.
     */
    public Set<String> getKeys() {
        return map.keySet();
    }

    /**
     * Clears all stored data (used for testing/reset).
     */
    public void clear() {
        map.clear();
    }
}
