package Components;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * Appends one or multiple elements to the right (tail) of a list key.
     * Returns the updated length of the list, or -1 if the key exists and is not a List.
     */
    public int rpush(String key, List<String> elements) {
        ConcurrentLinkedDeque<String> list = createList(key);
        if (list == null) {
            return -1; // Type mismatch (WRONGTYPE)
        }
        for (String el : elements) {
            list.addLast(el);
        }
        return list.size();
    }

    /**
     * Prepends one or multiple elements to the left (head) of a list key.
     * Returns the updated length of the list, or -1 if the key exists and is not a List.
     */
    public int lpush(String key, List<String> elements) {
        ConcurrentLinkedDeque<String> list = createList(key);
        if (list == null) {
            return -1; // Type mismatch (WRONGTYPE)
        }
        for (String el : elements) {
            list.addFirst(el);
        }
        return list.size();
    }

    /**
     * Returns a slice of list elements between start and stop indices (inclusive).
     * Handles positive indices (0-based) and negative indices (-1 for last element, -2 for 2nd last, etc.).
     * Returns null if the key exists and is not a List (WRONGTYPE).
     */
    public List<String> lrange(String key, int start, int stop) {
        Value val = get(key);
        if (val == null) {
            return Collections.emptyList();
        }
        if (!val.isList()) {
            return null; // Type mismatch (WRONGTYPE)
        }

        ConcurrentLinkedDeque<String> deque = val.getList();
        int size = deque.size();
        if (size == 0) {
            return Collections.emptyList();
        }

        // Normalize negative indices
        if (start < 0) {
            start = size + start;
        }
        if (stop < 0) {
            stop = size + stop;
        }

        // Clamp lower bounds
        if (start < 0) {
            start = 0;
        }

        // Clamp upper bounds
        if (stop >= size) {
            stop = size - 1;
        }

        // If start > stop or start >= size, return empty list
        if (start > stop || start >= size) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        int index = 0;
        for (String element : deque) {
            if (index >= start && index <= stop) {
                result.add(element);
            }
            index++;
            if (index > stop) {
                break;
            }
        }
        return result;
    }

    /**
     * Returns the length of the list at key.
     * Returns 0 if key does not exist, or -1 if key exists and is not a List.
     */
    public int llen(String key) {
        Value val = get(key);
        if (val == null) {
            return 0; // Non-existent key has length 0
        }
        if (!val.isList()) {
            return -1; // Type mismatch (WRONGTYPE)
        }
        return val.getList().size();
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
