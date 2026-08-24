package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CommandHandler evaluates parsed Redis commands and delegates data persistence
 * to the thread-safe Store component.
 */
@Component
public class CommandHandler {

    private final RespSerializer respSerializer;
    private final Store store;

    @Autowired
    public CommandHandler(RespSerializer respSerializer, Store store) {
        this.respSerializer = respSerializer;
        this.store = store;
    }

    /**
     * Processes a parsed command argument list and returns the RESP serialized response string.
     * 
     * @param commandParts List of strings where element 0 is the command name (e.g. ["LPOP", "mylist", "2"])
     * @return RESP formatted wire string (e.g. "$3\r\nfoo\r\n", "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n", "$-1\r\n", "-ERR ...")
     */
    public String handleCommand(List<String> commandParts) {
        if (commandParts == null || commandParts.isEmpty()) {
            return null;
        }

        String commandName = commandParts.get(0).toUpperCase();

        switch (commandName) {
            case "PING":
                // PING -> +PONG\r\n
                return respSerializer.encodeSimpleString("PONG");

            case "ECHO":
                // ECHO <message> -> $<len>\r\n<message>\r\n
                if (commandParts.size() > 1) {
                    return respSerializer.encodeBulkString(commandParts.get(1));
                }
                return respSerializer.encodeError("wrong number of arguments for 'echo' command");

            case "SET":
                // SET <key> <value> [EX seconds] [PX milliseconds]
                if (commandParts.size() < 3) {
                    return respSerializer.encodeError("wrong number of arguments for 'set' command");
                }

                String setKey = commandParts.get(1);
                String setVal = commandParts.get(2);
                Long ttlMillis = null;

                // Check for EX (seconds) or PX (milliseconds) options
                if (commandParts.size() >= 5) {
                    String option = commandParts.get(3).toUpperCase();
                    try {
                        long expireVal = Long.parseLong(commandParts.get(4));
                        if ("EX".equals(option)) {
                            ttlMillis = expireVal * 1000L;
                        } else if ("PX".equals(option)) {
                            ttlMillis = expireVal;
                        }
                    } catch (NumberFormatException e) {
                        return respSerializer.encodeError("value is not an integer or out of range");
                    }
                }

                store.set(setKey, setVal, ttlMillis);
                return respSerializer.encodeSimpleString("OK");

            case "GET":
                // GET <key> -> $<len>\r\n<val>\r\n or $-1\r\n (if non-existent or expired)
                if (commandParts.size() < 2) {
                    return respSerializer.encodeError("wrong number of arguments for 'get' command");
                }

                String getKey = commandParts.get(1);
                Value storedVal = store.get(getKey);

                if (storedVal == null || storedVal.getValue() == null) {
                    return respSerializer.encodeNullBulkString(); // $-1\r\n
                }

                return respSerializer.encodeBulkString(storedVal.getValue());

            case "RPUSH":
                // RPUSH key element [element ...] -> :<new_length>\r\n
                if (commandParts.size() < 3) {
                    return respSerializer.encodeError("wrong number of arguments for 'rpush' command");
                }
                String rpushKey = commandParts.get(1);
                List<String> rpushElements = commandParts.subList(2, commandParts.size());
                int rpushLen = store.rpush(rpushKey, rpushElements);
                if (rpushLen == -1) {
                    return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                return respSerializer.encodeInteger(rpushLen);

            case "LPUSH":
                // LPUSH key element [element ...] -> :<new_length>\r\n
                if (commandParts.size() < 3) {
                    return respSerializer.encodeError("wrong number of arguments for 'lpush' command");
                }
                String lpushKey = commandParts.get(1);
                List<String> lpushElements = commandParts.subList(2, commandParts.size());
                int lpushLen = store.lpush(lpushKey, lpushElements);
                if (lpushLen == -1) {
                    return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                return respSerializer.encodeInteger(lpushLen);

            case "LRANGE":
                // LRANGE key start stop -> *count\r\n$len\r\nel1\r\n...
                if (commandParts.size() < 4) {
                    return respSerializer.encodeError("wrong number of arguments for 'lrange' command");
                }
                String lrangeKey = commandParts.get(1);
                try {
                    int start = Integer.parseInt(commandParts.get(2));
                    int stop = Integer.parseInt(commandParts.get(3));
                    List<String> rangeResult = store.lrange(lrangeKey, start, stop);
                    if (rangeResult == null) {
                        return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                    }
                    return respSerializer.encodeArray(rangeResult);
                } catch (NumberFormatException e) {
                    return respSerializer.encodeError("value is not an integer or out of range");
                }

            case "LLEN":
                // LLEN key -> :length\r\n
                if (commandParts.size() < 2) {
                    return respSerializer.encodeError("wrong number of arguments for 'llen' command");
                }
                String llenKey = commandParts.get(1);
                int len = store.llen(llenKey);
                if (len == -1) {
                    return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                return respSerializer.encodeInteger(len);

            case "LPOP":
                // LPOP key [count]
                if (commandParts.size() < 2) {
                    return respSerializer.encodeError("wrong number of arguments for 'lpop' command");
                }
                String lpopKey = commandParts.get(1);
                int lpopCount = 1;
                boolean lpopHasCount = commandParts.size() >= 3;
                if (lpopHasCount) {
                    try {
                        lpopCount = Integer.parseInt(commandParts.get(2));
                    } catch (NumberFormatException e) {
                        return respSerializer.encodeError("value is not an integer or out of range");
                    }
                }

                List<String> lpopped = store.lpop(lpopKey, lpopCount);
                if (lpopped == null) {
                    return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                }

                if (!lpopHasCount) {
                    // Single element LPOP -> Bulk String ($len\r\nmsg\r\n) or Null Bulk String ($-1\r\n)
                    if (lpopped.isEmpty()) {
                        return respSerializer.encodeNullBulkString();
                    }
                    return respSerializer.encodeBulkString(lpopped.get(0));
                } else {
                    // Multi-element LPOP -> Array (*count\r\n...) or Null Array (*-1\r\n)
                    if (lpopped.isEmpty()) {
                        return respSerializer.encodeNullArray();
                    }
                    return respSerializer.encodeArray(lpopped);
                }

            case "RPOP":
                // RPOP key [count]
                if (commandParts.size() < 2) {
                    return respSerializer.encodeError("wrong number of arguments for 'rpop' command");
                }
                String rpopKey = commandParts.get(1);
                int rpopCount = 1;
                boolean rpopHasCount = commandParts.size() >= 3;
                if (rpopHasCount) {
                    try {
                        rpopCount = Integer.parseInt(commandParts.get(2));
                    } catch (NumberFormatException e) {
                        return respSerializer.encodeError("value is not an integer or out of range");
                    }
                }

                List<String> rpopped = store.rpop(rpopKey, rpopCount);
                if (rpopped == null) {
                    return respSerializer.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
                }

                if (!rpopHasCount) {
                    // Single element RPOP -> Bulk String ($len\r\nmsg\r\n) or Null Bulk String ($-1\r\n)
                    if (rpopped.isEmpty()) {
                        return respSerializer.encodeNullBulkString();
                    }
                    return respSerializer.encodeBulkString(rpopped.get(0));
                } else {
                    // Multi-element RPOP -> Array (*count\r\n...) or Null Array (*-1\r\n)
                    if (rpopped.isEmpty()) {
                        return respSerializer.encodeNullArray();
                    }
                    return respSerializer.encodeArray(rpopped);
                }

            default:
                return respSerializer.encodeError("unknown command '" + commandParts.get(0) + "'");
        }
    }
}
