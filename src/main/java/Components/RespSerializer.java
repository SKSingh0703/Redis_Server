package Components;

import org.springframework.stereotype.Component;

/**
 * Responsible for encoding data into standard Redis Serialization Protocol (RESP) formats.
 * Marked with @Component so Spring automatically detects and manages it as a Bean.
 */
@Component
public class RespSerializer {

    /**
     * Formats a String into a RESP Simple String (+<message>\r\n).
     * Example: "PONG" -> "+PONG\r\n"
     */
    public String encodeSimpleString(String s) {
        return "+" + s + "\r\n";
    }

    /**
     * Formats a String into a RESP Bulk String ($<length>\r\n<message>\r\n).
     * Example: "hello" -> "$5\r\nhello\r\n"
     */
    public String encodeBulkString(String s) {
        if (s == null) {
            return "$-1\r\n"; // Null Bulk String
        }
        return "$" + s.length() + "\r\n" + s + "\r\n";
    }

    /**
     * Formats an integer into a RESP Integer (:<number>\r\n).
     * Example: 100 -> ":100\r\n"
     */
    public String encodeInteger(long value) {
        return ":" + value + "\r\n";
    }

    /**
     * Formats an error message into a RESP Error (-ERR <message>\r\n).
     * Example: "unknown command" -> "-ERR unknown command\r\n"
     */
    public String encodeError(String message) {
        return "-ERR " + message + "\r\n";
    }
}
