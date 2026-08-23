import Components.DataType;
import Components.Store;
import Components.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    public void testCreateListOnExistingListKeyReturnsSameList() {
        ConcurrentLinkedDeque<String> list1 = store.createList("mylist");
        list1.add("element1");

        ConcurrentLinkedDeque<String> list2 = store.createList("mylist");
        assertNotNull(list2);
        assertEquals(1, list2.size());
        assertEquals("element1", list2.peek());
    }

    @Test
    public void testCreateListOnExistingStringKeyFailsTypeCheck() {
        store.set("strKey", "hello");

        // Attempting to create a list on a key holding a String payload should return null (type mismatch)
        ConcurrentLinkedDeque<String> list = store.createList("strKey");
        assertNull(list);
    }
}
