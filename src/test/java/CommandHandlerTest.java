import Components.CommandHandler;
import Components.RespSerializer;
import Components.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandHandlerTest {

    private CommandHandler commandHandler;

    @BeforeEach
    public void setUp() {
        RespSerializer respSerializer = new RespSerializer();
        Store store = new Store();
        commandHandler = new CommandHandler(respSerializer, store);
    }

    @Test
    public void testHandlePingCommand() {
        String response = commandHandler.handleCommand(List.of("PING"));
        assertEquals("+PONG\r\n", response);
    }

    @Test
    public void testHandleEchoCommandValid() {
        String response = commandHandler.handleCommand(List.of("ECHO", "hello world"));
        assertEquals("$11\r\nhello world\r\n", response);
    }

    @Test
    public void testHandleEchoCommandMissingArgs() {
        String response = commandHandler.handleCommand(List.of("ECHO"));
        assertEquals("-ERR wrong number of arguments for 'echo' command\r\n", response);
    }

    @Test
    public void testHandleSetAndGetCommandValid() {
        String setResp = commandHandler.handleCommand(List.of("SET", "orange", "raspberry"));
        assertEquals("+OK\r\n", setResp);

        String getResp = commandHandler.handleCommand(List.of("GET", "orange"));
        assertEquals("$9\r\nraspberry\r\n", getResp);
    }

    @Test
    public void testHandleSetWithPxExpiration() throws InterruptedException {
        // SET key val PX 100
        String setResp = commandHandler.handleCommand(List.of("SET", "foo", "bar", "PX", "100"));
        assertEquals("+OK\r\n", setResp);

        // Immediate GET -> Should return value
        String getResp1 = commandHandler.handleCommand(List.of("GET", "foo"));
        assertEquals("$3\r\nbar\r\n", getResp1);

        // Sleep 150ms -> Key should expire
        Thread.sleep(150);

        // Subsequent GET -> Should return Null Bulk String ($-1\r\n)
        String getResp2 = commandHandler.handleCommand(List.of("GET", "foo"));
        assertEquals("$-1\r\n", getResp2);
    }

    @Test
    public void testHandleRpushSingleAndMultipleElements() {
        // RPUSH mylist foo -> returns :1\r\n
        String r1 = commandHandler.handleCommand(List.of("RPUSH", "mylist", "foo"));
        assertEquals(":1\r\n", r1);

        // RPUSH mylist bar baz -> returns :3\r\n
        String r2 = commandHandler.handleCommand(List.of("RPUSH", "mylist", "bar", "baz"));
        assertEquals(":3\r\n", r2);
    }

    @Test
    public void testHandleLpushSingleAndMultipleElements() {
        // LPUSH mylist foo -> returns :1\r\n
        String r1 = commandHandler.handleCommand(List.of("LPUSH", "mylist", "foo"));
        assertEquals(":1\r\n", r1);

        // LPUSH mylist bar -> returns :2\r\n
        String r2 = commandHandler.handleCommand(List.of("LPUSH", "mylist", "bar"));
        assertEquals(":2\r\n", r2);
    }

    @Test
    public void testHandleRpushOnStringKeyReturnsWrongType() {
        commandHandler.handleCommand(List.of("SET", "strKey", "hello"));
        String response = commandHandler.handleCommand(List.of("RPUSH", "strKey", "world"));
        assertEquals("-ERR WRONGTYPE Operation against a key holding the wrong kind of value\r\n", response);
    }

    @Test
    public void testHandleSetCommandMissingArgs() {
        String response = commandHandler.handleCommand(List.of("SET", "orange"));
        assertEquals("-ERR wrong number of arguments for 'set' command\r\n", response);
    }

    @Test
    public void testHandleGetCommandNonExistent() {
        String response = commandHandler.handleCommand(List.of("GET", "non_existent"));
        assertEquals("$-1\r\n", response);
    }

    @Test
    public void testHandleUnknownCommand() {
        String response = commandHandler.handleCommand(List.of("INVALID_CMD"));
        assertEquals("-ERR unknown command 'INVALID_CMD'\r\n", response);
    }
}
