package Components;

/**
 * Value encapsulates a stored string payload along with an optional expiration timestamp (TTL).
 * If expiryTimestamp is null, the value persists indefinitely until explicitly deleted or overwritten.
 */
public class Value {

    private final String value;
    private final Long expiryTimestamp; // Expiry epoch timestamp in milliseconds

    /**
     * Constructs a persistent Value without expiration.
     */
    public Value(String value) {
        this.value = value;
        this.expiryTimestamp = null;
    }

    /**
     * Constructs a Value with a specific expiration timestamp in epoch milliseconds.
     */
    public Value(String value, Long expiryTimestamp) {
        this.value = value;
        this.expiryTimestamp = expiryTimestamp;
    }

    public String getValue() {
        return value;
    }

    public Long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    /**
     * Checks whether this value has exceeded its expiration timestamp.
     * 
     * @return true if an expiration timestamp is set and the current system time is past it.
     */
    public boolean isExpired() {
        return expiryTimestamp != null && System.currentTimeMillis() > expiryTimestamp;
    }
}
