package Components;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Store is the in-memory database component managing key-value pairs (Strings and Lists).
 * 
 * Thread-Safety: Uses ConcurrentHashMap and ConcurrentLinkedDeque to allow lock-safe concurrent
 * reads and writes across worker client threads.
 */
@Component
public class Store {

    private final ConcurrentHashMap<String, Value> map;

    public Store() {
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * Stores a String key-value pair without expiration (persistent).
     */
    public void set(String key, String value) {
        map.put(key, new Value(value));
    }

    /**
     * Stores a String key-value pair with a Time-To-Live (TTL) in milliseconds.
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
     * Creates a new empty List Value at the specified key.
     * If the key already exists and is a List, returns the existing deque.
     * If the key exists and is a String (wrong type), returns null.
     */
    public ConcurrentLinkedDeque<String> createList(String key) {
        Value existing = get(key);
        if (existing != null) {
            if (!existing.isList()) {
                return null; // Type mismatch (WRONGTYPE)
            }
            return existing.getList();
        }

        ConcurrentLinkedDeque<String> newList = new ConcurrentLinkedDeque<>();
        map.put(key, new Value(newList));
        return newList;
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
