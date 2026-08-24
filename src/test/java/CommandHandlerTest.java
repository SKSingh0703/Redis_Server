import Components.Repository.Store;
import Components.Server.RespSerializer;
import Components.Service.CommandHandler;
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
    public void testHandleRpushAndLrangePositiveAndNegativeIndices() {
        commandHandler.handleCommand(List.of("RPUSH", "mylist", "foo", "bar", "baz"));

        // LRANGE mylist 0 0 -> *1\r\n$3\r\nfoo\r\n
        String r1 = commandHandler.handleCommand(List.of("LRANGE", "mylist", "0", "0"));
        assertEquals("*1\r\n$3\r\nfoo\r\n", r1);

        // LRANGE mylist 0 -1 -> *3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n
        String r2 = commandHandler.handleCommand(List.of("LRANGE", "mylist", "0", "-1"));
        assertEquals("*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n", r2);

        // LRANGE mylist -2 -1 -> *2\r\n$3\r\nbar\r\n$3\r\nbaz\r\n
        String r3 = commandHandler.handleCommand(List.of("LRANGE", "mylist", "-2", "-1"));
        assertEquals("*2\r\n$3\r\nbar\r\n$3\r\nbaz\r\n", r3);
    }

    @Test
    public void testHandleLpopSingleAndMultipleElements() {
        commandHandler.handleCommand(List.of("RPUSH", "mylist", "one", "two", "three"));

        // LPOP mylist (single element) -> returns Bulk String $3\r\none\r\n
        String r1 = commandHandler.handleCommand(List.of("LPOP", "mylist"));
        assertEquals("$3\r\none\r\n", r1);

        // LPOP mylist 2 (multiple elements) -> returns Array *2\r\n$3\r\ntwo\r\n$5\r\nthree\r\n
        String r2 = commandHandler.handleCommand(List.of("LPOP", "mylist", "2"));
        assertEquals("*2\r\n$3\r\ntwo\r\n$5\r\nthree\r\n", r2);

        // LPOP mylist on now-empty key -> returns Null Bulk String $-1\r\n
        String r3 = commandHandler.handleCommand(List.of("LPOP", "mylist"));
        assertEquals("$-1\r\n", r3);

        // LPOP mylist 2 on now-empty key -> returns Null Array *-1\r\n
        String r4 = commandHandler.handleCommand(List.of("LPOP", "mylist", "2"));
        assertEquals("*-1\r\n", r4);
    }

    @Test
    public void testHandleLlenCommand() {
        commandHandler.handleCommand(List.of("RPUSH", "mylist", "a", "b", "c"));
        String lenResp = commandHandler.handleCommand(List.of("LLEN", "mylist"));
        assertEquals(":3\r\n", lenResp);
    }

    @Test
    public void testHandleLrangeOnStringKeyReturnsWrongType() {
        commandHandler.handleCommand(List.of("SET", "strKey", "hello"));
        String response = commandHandler.handleCommand(List.of("LRANGE", "strKey", "0", "-1"));
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
