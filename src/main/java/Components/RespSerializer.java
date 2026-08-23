package Components;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RespSerializer is responsible for encoding Java objects into Redis Serialization Protocol (RESP) strings
 * and decoding (parsing) incoming RESP byte streams into structured Redis command parameter lists.
 * 
 * Protocol Types (RESP2 / RESP3):
 *  '+' -> Simple Strings (non binary-safe string + \r\n)
 *  '-' -> Errors (-ERR message \r\n)
 *  ':' -> Integers (:1000 \r\n)
 *  '$' -> Bulk Strings ($<length>\r\n<data>\r\n)
 *  '*' -> Arrays (*<count>\r\n<element1>...)
 */
@Component
public class RespSerializer {

    // ==========================================
    // SERIALIZATION (ENCODING) METHODS
    // ==========================================

    /**
     * Formats a String into a RESP Simple String (+<message>\r\n).
     * Example: "PONG" -> "+PONG\r\n"
     */
    public String encodeSimpleString(String s) {
        return "+" + s + "\r\n";
    }

    /**
     * Formats a String into a RESP Bulk String ($<length>\r\n<message>\r\n).
     * Binary-safe format.
     * Example: "hello" -> "$5\r\nhello\r\n"
     */
    public String encodeBulkString(String s) {
        if (s == null) {
            return encodeNullBulkString();
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return "$" + bytes.length + "\r\n" + s + "\r\n";
    }

    /**
     * Formats a Null Bulk String ($-1\r\n).
     * Used when a requested key does not exist.
     */
    public String encodeNullBulkString() {
        return "$-1\r\n";
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

    /**
     * Formats a list of elements into a RESP Array (*<count>\r\n...).
     * Example: ["SET", "key", "val"] -> "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$3\r\nval\r\n"
     */
    public String encodeArray(List<String> elements) {
        if (elements == null) {
            return "*-1\r\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(elements.size()).append("\r\n");
        for (String el : elements) {
            sb.append(encodeBulkString(el));
        }
        return sb.toString();
    }

    // ==========================================
    // DESERIALIZATION (PARSING) METHODS
    // ==========================================

    /**
     * Reads a raw RESP byte stream from an InputStream and parses it into a List of String arguments.
     * Handles RESP Arrays (*), Bulk Strings ($), Simple Strings (+), and Fallback Inline Text Commands.
     * 
     * @param inputStream the network input stream from the connected client
     * @return List of command parts (e.g. ["SET", "orange", "raspberry"]) or null if client disconnected (EOF)
     */
    public List<String> deserialize(InputStream inputStream) throws IOException {
        int firstByte = inputStream.read();
        if (firstByte == -1) {
            return null; // Connection closed by client (EOF)
        }

        char prefix = (char) firstByte;

        switch (prefix) {
            case '*': {
                // RESP Array: *<count>\r\n
                String countStr = readLine(inputStream);
                if (countStr == null || countStr.isEmpty()) return Collections.emptyList();
                
                int count = Integer.parseInt(countStr.trim());
                if (count <= 0) return Collections.emptyList();

                List<String> commandParts = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    List<String> element = deserialize(inputStream);
                    if (element != null && !element.isEmpty()) {
                        commandParts.add(element.get(0));
                    }
                }
                return commandParts;
            }
            case '$': {
                // RESP Bulk String: $<length>\r\n<data>\r\n
                String lenStr = readLine(inputStream);
                if (lenStr == null || lenStr.isEmpty()) return Collections.singletonList(null);
                
                int length = Integer.parseInt(lenStr.trim());
                if (length < 0) {
                    return Collections.singletonList(null); // Null Bulk String ($-1)
                }

                byte[] payload = new byte[length];
                int bytesRead = 0;
                while (bytesRead < length) {
                    int read = inputStream.read(payload, bytesRead, length - bytesRead);
                    if (read == -1) break;
                    bytesRead += read;
                }

                // Consume trailing CRLF (\r\n)
                int r = inputStream.read();
                if (r == '\r') {
                    inputStream.read(); // consume \n
                }

                return Collections.singletonList(new String(payload, StandardCharsets.UTF_8));
            }
            case '+':
            case ':':
            case '-': {
                // Simple String (+), Integer (:), Error (-)
                String line = readLine(inputStream);
                return Collections.singletonList(line);
            }
            default: {
                // Fallback for Inline / Plain Text Commands (e.g., telnet "PING" or "ECHO hello")
                String remainder = readLine(inputStream);
                String fullLine = prefix + (remainder != null ? remainder : "");
                String[] parts = fullLine.trim().split("\\s+");
                return Arrays.asList(parts);
            }
        }
    }

    /**
     * Reads bytes from stream until CRLF (\r\n) or LF (\n) is encountered.
     */
    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.mark(1);
                int next = in.read();
                if (next != '\n') {
                    baos.write(b);
                    if (next != -1) baos.write(next);
                } else {
                    break;
                }
            } else if (b == '\n') {
                break;
            } else {
                baos.write(b);
            }
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
