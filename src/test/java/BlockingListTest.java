import Components.CommandHandler;
import Components.RespSerializer;
import Components.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class BlockingListTest {

    private Store store;
    private CommandHandler commandHandler;

    @BeforeEach
    public void setUp() {
        store = new Store();
        RespSerializer serializer = new RespSerializer();
        commandHandler = new CommandHandler(serializer, store);
    }

    @Test
    public void testImmediateBlpopWhenDataExists() {
        store.rpush("mylist", List.of("apple", "banana"));

        List<String> result = store.blpop(List.of("mylist"), 0.1);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("mylist", result.get(0));
        assertEquals("apple", result.get(1));
    }

    @Test
    public void testBlpopTimeoutExpiration() {
        long start = System.currentTimeMillis();
        List<String> result = store.blpop(List.of("emptykey"), 0.1);
        long elapsed = System.currentTimeMillis() - start;

        assertNull(result);
        assertTrue(elapsed >= 90, "Elapsed time should be ~100ms but was " + elapsed);
    }

    @Test
    public void testBlpopCrossThreadNotification() throws InterruptedException {
        AtomicReference<String> blpopResponse = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Worker Thread A: Calls BLPOP and blocks
        Thread workerA = new Thread(() -> {
            String resp = commandHandler.handleCommand(List.of("BLPOP", "mylist", "2"));
            blpopResponse.set(resp);
            latch.countDown();
        });
        workerA.start();

        // Sleep 150ms to ensure Thread A is waiting
        Thread.sleep(150);

        // Main Thread B: Calls RPUSH to wake up Worker Thread A
        commandHandler.handleCommand(List.of("RPUSH", "mylist", "hello_world"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("*2\r\n$6\r\nmylist\r\n$11\r\nhello_world\r\n", blpopResponse.get());
    }
}
