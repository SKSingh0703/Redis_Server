package Components.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Store is the in-memory repository component managing key-value pairs (Strings and Lists).
 * Located in package Components.Repository.
 * 
 * Thread-Safety & Blocking: Uses ConcurrentHashMap, ConcurrentLinkedDeque, and per-key Monitor locks
 * to support lock-safe concurrent reads/writes and Producer-Consumer blocking retrievals (BLPOP/BRPOP).
 */
@Component
public class Store {

    private static final Logger logger = LoggerFactory.getLogger(Store.class);

    private final ConcurrentHashMap<String, Value> map;
    // Lock for each key to support blocking operations and prevent multiple threads from accessing the same key
    private final ConcurrentHashMap<String, Object> keyLocks;

    public Store() {
        this.map = new ConcurrentHashMap<>();
        this.keyLocks = new ConcurrentHashMap<>();
    }

    private Object getKeyLock(String key) {
        return keyLocks.computeIfAbsent(key, k -> new Object());
    }

    private void notifyKey(String key) {
        Object lock = keyLocks.get(key);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Stores a String key-value pair without expiration (persistent).
     */
    public void set(String key, String value) {
        map.put(key, new Value(value));
        notifyKey(key);
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
            notifyKey(key);
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
     * Notifies any waiting BLPOP/BRPOP threads.
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
        notifyKey(key);
        return list.size();
    }

    /**
     * Prepends one or multiple elements to the left (head) of a list key.
     * Notifies any waiting BLPOP/BRPOP threads.
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
        notifyKey(key);
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
     * Removes and returns up to count elements from the left (head) of a list key.
     * Automatically deletes the key if the list becomes empty.
     * Returns null if key exists and is not a List (WRONGTYPE).
     */
    public List<String> lpop(String key, int count) {
        Value val = get(key);
        if (val == null) {
            return Collections.emptyList();
        }
        if (!val.isList()) {
            return null; // Type mismatch (WRONGTYPE)
        }

        ConcurrentLinkedDeque<String> deque = val.getList();
        if (deque.isEmpty()) {
            delete(key);
            return Collections.emptyList();
        }

        List<String> popped = new ArrayList<>();
        int toPop = Math.min(count, deque.size());
        for (int i = 0; i < toPop; i++) {
            String el = deque.pollFirst();
            if (el != null) {
                popped.add(el);
            }
        }

        // Automatic key deletion when list becomes empty
        if (deque.isEmpty()) {
            delete(key);
        }

        return popped;
    }

    /**
     * Removes and returns up to count elements from the right (tail) of a list key.
     * Automatically deletes the key if the list becomes empty.
     * Returns null if key exists and is not a List (WRONGTYPE).
     */
    public List<String> rpop(String key, int count) {
        Value val = get(key);
        if (val == null) {
            return Collections.emptyList();
        }
        if (!val.isList()) {
            return null; // Type mismatch (WRONGTYPE)
        }

        ConcurrentLinkedDeque<String> deque = val.getList();
        if (deque.isEmpty()) {
            delete(key);
            return Collections.emptyList();
        }

        List<String> popped = new ArrayList<>();
        int toPop = Math.min(count, deque.size());
        for (int i = 0; i < toPop; i++) {
            String el = deque.pollLast();
            if (el != null) {
                popped.add(el);
            }
        }

        // Automatic key deletion when list becomes empty
        if (deque.isEmpty()) {
            delete(key);
        }

        return popped;
    }

    /**
     * Blocking Left Pop: Removes and gets the first element from one of the specified keys.
     * Blocks if all keys are empty until an element is pushed or timeout (in seconds) expires.
     * If timeout is 0, blocks indefinitely.
     * Returns a 2-element list [keyName, poppedElement], or null if timed out.
     */
    public List<String> blpop(List<String> keys, double timeoutSeconds) {
        long timeoutMs = (long) (timeoutSeconds * 1000L);
        boolean indefinite = (timeoutSeconds == 0);
        long deadline = indefinite ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;

        while (true) {
            // 1. Immediate non-blocking check across all target keys
            for (String key : keys) {
                List<String> popped = lpop(key, 1);
                if (popped != null && !popped.isEmpty()) {
                    return List.of(key, popped.get(0));
                }
            }

            // 2. Timeout check
            long remaining = deadline - System.currentTimeMillis();
            if (!indefinite && remaining <= 0) {
                return null; // Timed out
            }

            // 3. Wait on key lock monitors
            for (String key : keys) {
                Object lock = getKeyLock(key);
                synchronized (lock) {
                    try {
                        long waitTime = indefinite ? 100L : Math.min(remaining, 100L);
                        if (waitTime > 0) {
                            lock.wait(waitTime);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
    }

    /**
     * Blocking Right Pop: Removes and gets the last element from one of the specified keys.
     * Blocks if all keys are empty until an element is pushed or timeout (in seconds) expires.
     * If timeout is 0, blocks indefinitely.
     * Returns a 2-element list [keyName, poppedElement], or null if timed out.
     */
    public List<String> brpop(List<String> keys, double timeoutSeconds) {
        long timeoutMs = (long) (timeoutSeconds * 1000L);
        boolean indefinite = (timeoutSeconds == 0);
        long deadline = indefinite ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;

        while (true) {
            // 1. Immediate non-blocking check across all target keys
            for (String key : keys) {
                List<String> popped = rpop(key, 1);
                if (popped != null && !popped.isEmpty()) {
                    return List.of(key, popped.get(0));
                }
            }

            // 2. Timeout check
            long remaining = deadline - System.currentTimeMillis();
            if (!indefinite && remaining <= 0) {
                return null; // Timed out
            }

            // 3. Wait on key lock monitors
            for (String key : keys) {
                Object lock = getKeyLock(key);
                synchronized (lock) {
                    try {
                        long waitTime = indefinite ? 100L : Math.min(remaining, 100L);
                        if (waitTime > 0) {
                            lock.wait(waitTime);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
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
            logger.debug("Passive eviction triggered for expired key: {}", key);
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
