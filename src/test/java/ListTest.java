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
        assertEquals("apple", val.getList().pollFirst());
        assertEquals("banana", val.getList().pollFirst());
        assertEquals("cherry", val.getList().pollFirst());
        assertEquals("dragonfruit", val.getList().pollFirst());
    }

    @Test
    public void testLpushSingleAndMultipleElements() {
        int len1 = store.lpush("numbers", List.of("one"));
        assertEquals(1, len1);

        // lpush "two", "three" -> "three" then "two" prepended to head
        int len2 = store.lpush("numbers", List.of("two", "three"));
        assertEquals(3, len2);

        Value val = store.get("numbers");
        assertNotNull(val);
        assertEquals(3, val.getList().size());
        // Since LPUSH prepends elements in sequence, "three" was pushed last to head
        assertEquals("three", val.getList().pollFirst());
        assertEquals("two", val.getList().pollFirst());
        assertEquals("one", val.getList().pollFirst());
    }

    @Test
    public void testRpushOnStringKeyReturnsWrongType() {
        store.set("strKey", "hello");

        int len = store.rpush("strKey", List.of("world"));
        assertEquals(-1, len);
    }
}
