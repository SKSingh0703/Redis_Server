import Components.Server.RespSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RespSerializerTest {

    private RespSerializer serializer;

    @BeforeEach
    public void setUp() {
        serializer = new RespSerializer();
    }

    @Test
    public void testEncodeSimpleString() {
        assertEquals("+OK\r\n", serializer.encodeSimpleString("OK"));
        assertEquals("+PONG\r\n", serializer.encodeSimpleString("PONG"));
    }

    @Test
    public void testEncodeBulkString() {
        assertEquals("$5\r\nhello\r\n", serializer.encodeBulkString("hello"));
        assertEquals("$-1\r\n", serializer.encodeBulkString(null));
    }

    @Test
    public void testEncodeInteger() {
        assertEquals(":1000\r\n", serializer.encodeInteger(1000));
        assertEquals(":-5\r\n", serializer.encodeInteger(-5));
    }

    @Test
    public void testEncodeError() {
        assertEquals("-ERR unknown command\r\n", serializer.encodeError("unknown command"));
    }

    @Test
    public void testEncodeArray() {
        List<String> list = List.of("SET", "key", "val");
        String expected = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$3\r\nval\r\n";
        assertEquals(expected, serializer.encodeArray(list));
    }

    @Test
    public void testDeserializeRespArraySetCommand() throws IOException {
        String rawInput = "*3\r\n$3\r\nSET\r\n$6\r\norange\r\n$9\r\nraspberry\r\n";
        InputStream in = new ByteArrayInputStream(rawInput.getBytes(StandardCharsets.UTF_8));

        List<String> command = serializer.deserialize(in);

        assertNotNull(command);
        assertEquals(3, command.size());
        assertEquals("SET", command.get(0));
        assertEquals("orange", command.get(1));
        assertEquals("raspberry", command.get(2));
    }

    @Test
    public void testDeserializeRespArrayEchoCommand() throws IOException {
        String rawInput = "*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n";
        InputStream in = new ByteArrayInputStream(rawInput.getBytes(StandardCharsets.UTF_8));

        List<String> command = serializer.deserialize(in);

        assertNotNull(command);
        assertEquals(2, command.size());
        assertEquals("ECHO", command.get(0));
        assertEquals("hello", command.get(1));
    }

    @Test
    public void testDeserializeInlinePingCommand() throws IOException {
        String rawInput = "PING\r\n";
        InputStream in = new ByteArrayInputStream(rawInput.getBytes(StandardCharsets.UTF_8));

        List<String> command = serializer.deserialize(in);

        assertNotNull(command);
        assertEquals(1, command.size());
        assertEquals("PING", command.get(0));
    }
}
