import Components.CommandHandler;
import Components.RespSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandHandlerTest {

    private CommandHandler commandHandler;

    @BeforeEach
    public void setUp() {
        RespSerializer respSerializer = new RespSerializer();
        commandHandler = new CommandHandler(respSerializer);
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
    public void testHandleSetCommandValid() {
        String response = commandHandler.handleCommand(List.of("SET", "orange", "raspberry"));
        assertEquals("+OK\r\n", response);
    }

    @Test
    public void testHandleSetCommandMissingArgs() {
        String response = commandHandler.handleCommand(List.of("SET", "orange"));
        assertEquals("-ERR wrong number of arguments for 'set' command\r\n", response);
    }

    @Test
    public void testHandleGetCommandValid() {
        String response = commandHandler.handleCommand(List.of("GET", "orange"));
        assertEquals("$-1\r\n", response);
    }

    @Test
    public void testHandleUnknownCommand() {
        String response = commandHandler.handleCommand(List.of("INVALID_CMD"));
        assertEquals("-ERR unknown command 'INVALID_CMD'\r\n", response);
    }
}
