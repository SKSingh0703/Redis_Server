package Components.Repository;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Value encapsulates a stored Redis data payload (String or List) along with an optional expiration timestamp (TTL).
 * Located in package Components.Repository.
 */
public class Value {

    private final DataType type;
    private final Object data;
    private final Long expiryTimestamp;

    /**
     * Constructs a persistent String Value.
     */
    public Value(String value) {
        this(DataType.STRING, value, null);
    }

    /**
     * Constructs a String Value with an expiration timestamp.
     */
    public Value(String value, Long expiryTimestamp) {
        this(DataType.STRING, value, expiryTimestamp);
    }

    /**
     * Constructs a persistent List Value.
     */
    public Value(ConcurrentLinkedDeque<String> list) {
        this(DataType.LIST, list, null);
    }

    /**
     * Constructs a List Value with an expiration timestamp.
     */
    public Value(ConcurrentLinkedDeque<String> list, Long expiryTimestamp) {
        this(DataType.LIST, list, expiryTimestamp);
    }

    private Value(DataType type, Object data, Long expiryTimestamp) {
        this.type = type;
        this.data = data;
        this.expiryTimestamp = expiryTimestamp;
    }

    public DataType getType() {
        return type;
    }

    public boolean isString() {
        return type == DataType.STRING;
    }

    public boolean isList() {
        return type == DataType.LIST;
    }

    /**
     * Returns the string payload if type is STRING, otherwise null.
     */
    public String getValue() {
        return isString() ? (String) data : null;
    }

    /**
     * Returns the concurrent deque list payload if type is LIST, otherwise null.
     */
    @SuppressWarnings("unchecked")
    public ConcurrentLinkedDeque<String> getList() {
        return isList() ? (ConcurrentLinkedDeque<String>) data : null;
    }

    public Long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    /**
     * Checks whether this value has exceeded its expiration timestamp.
     */
    public boolean isExpired() {
        return expiryTimestamp != null && System.currentTimeMillis() > expiryTimestamp;
    }
}
