package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CommandHandler is responsible for processing parsed Redis commands and returning
 * the appropriate RESP protocol responses.
 * 
 * By encapsulating command evaluation here, TcpServer is decoupled from business logic
 * (Command / Dispatcher Pattern).
 */
@Component
public class CommandHandler {

    private final RespSerializer respSerializer;

    @Autowired
    public CommandHandler(RespSerializer respSerializer) {
        this.respSerializer = respSerializer;
    }

    /**
     * Processes a parsed command argument list and returns the RESP serialized response string.
     * 
     * @param commandParts List of strings where element 0 is the command name (e.g. ["SET", "key", "val"])
     * @return RESP formatted wire string (e.g. "+OK\r\n", "$5\r\nhello\r\n", "-ERR unknown command\r\n")
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
                // SET <key> <value> -> +OK\r\n
                if (commandParts.size() >= 3) {
                    return respSerializer.encodeSimpleString("OK");
                }
                return respSerializer.encodeError("wrong number of arguments for 'set' command");

            case "GET":
                // GET <key> -> Null Bulk String if not found ($-1\r\n)
                if (commandParts.size() >= 2) {
                    return respSerializer.encodeNullBulkString();
                }
                return respSerializer.encodeError("wrong number of arguments for 'get' command");

            default:
                return respSerializer.encodeError("unknown command '" + commandParts.get(0) + "'");
        }
    }
}
