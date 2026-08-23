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
     * @param commandParts List of strings where element 0 is the command name (e.g. ["SET", "key", "val", "EX", "10"])
     * @return RESP formatted wire string (e.g. "+OK\r\n", "$5\r\nhello\r\n", "$-1\r\n", "-ERR ...")
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

            default:
                return respSerializer.encodeError("unknown command '" + commandParts.get(0) + "'");
        }
    }
}
