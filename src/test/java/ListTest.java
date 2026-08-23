import Components.DataType;
import Components.Store;
import Components.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.*;

public class ListTest {

    private Store store;

    @BeforeEach
    public void setUp() {
        store = new Store();
    }

    @Test
    public void testCreateEmptyList() {
        ConcurrentLinkedDeque<String> list = store.createList("mylist");

        assertNotNull(list);
        assertTrue(list.isEmpty());

        Value val = store.get("mylist");
        assertNotNull(val);
        assertEquals(DataType.LIST, val.getType());
        assertTrue(val.isList());
        assertFalse(val.isString());
        assertNotNull(val.getList());
    }

    @Test
    public void testRpushSingleElement() {
        int len = store.rpush("fruits", List.of("apple"));
        assertEquals(1, len);

        Value val = store.get("fruits");
        assertNotNull(val);
        assertEquals(1, val.getList().size());
        assertEquals("apple", val.getList().peekFirst());
    }

    @Test
    public void testRpushMultipleElements() {
        int len1 = store.rpush("fruits", List.of("apple"));
        assertEquals(1, len1);

        int len2 = store.rpush("fruits", List.of("banana", "cherry", "dragonfruit"));
        assertEquals(4, len2);

        Value val = store.get("fruits");
        assertNotNull(val);
        assertEquals(4, val.getList().size());
    }

    @Test
    public void testLpushSingleAndMultipleElements() {
        int len1 = store.lpush("numbers", List.of("one"));
        assertEquals(1, len1);

        int len2 = store.lpush("numbers", List.of("two", "three"));
        assertEquals(3, len2);

        Value val = store.get("numbers");
        assertNotNull(val);
        assertEquals(3, val.getList().size());
    }

    @Test
    public void testLrangePositiveAndNegativeIndices() {
        // RPUSH mylist "a", "b", "c", "d"
        store.rpush("mylist", List.of("a", "b", "c", "d"));

        // LRANGE mylist 0 0 -> ["a"]
        List<String> r1 = store.lrange("mylist", 0, 0);
        assertEquals(List.of("a"), r1);

        // LRANGE mylist 0 -1 -> ["a", "b", "c", "d"]
        List<String> r2 = store.lrange("mylist", 0, -1);
        assertEquals(List.of("a", "b", "c", "d"), r2);

        // LRANGE mylist 0 2 -> ["a", "b", "c"]
        List<String> r3 = store.lrange("mylist", 0, 2);
        assertEquals(List.of("a", "b", "c"), r3);

        // LRANGE mylist -2 -1 -> ["c", "d"]
        List<String> r4 = store.lrange("mylist", -2, -1);
        assertEquals(List.of("c", "d"), r4);

        // Out of bound stop index: LRANGE mylist 0 100 -> ["a", "b", "c", "d"]
        List<String> r5 = store.lrange("mylist", 0, 100);
        assertEquals(List.of("a", "b", "c", "d"), r5);

        // Out of bound start index: LRANGE mylist 10 20 -> []
        List<String> r6 = store.lrange("mylist", 10, 20);
        assertTrue(r6.isEmpty());
    }

    @Test
    public void testLlenCommand() {
        store.rpush("mylist", List.of("one", "two", "three"));
        assertEquals(3, store.llen("mylist"));

        // Non-existent key -> length 0
        assertEquals(0, store.llen("non_existent"));

        // String key -> returns -1 (WRONGTYPE)
        store.set("strKey", "hello");
        assertEquals(-1, store.llen("strKey"));
    }

    @Test
    public void testRpushOnStringKeyReturnsWrongType() {
        store.set("strKey", "hello");

        int len = store.rpush("strKey", List.of("world"));
        assertEquals(-1, len);
    }
}
