import Components.Repository.Store;
import Components.Repository.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StoreTest {

    private Store store;

    @BeforeEach
    public void setUp() {
        store = new Store();
    }

    @Test
    public void testSetAndGetPersistentKey() {
        store.set("fruit", "apple");
        Value val = store.get("fruit");

        assertNotNull(val);
        assertEquals("apple", val.getValue());
        assertFalse(val.isExpired());
    }

    @Test
    public void testGetNonExistentKey() {
        Value val = store.get("non_existent");
        assertNull(val);
    }

    @Test
    public void testOverwriteKey() {
        store.set("fruit", "apple");
        store.set("fruit", "banana");

        Value val = store.get("fruit");
        assertNotNull(val);
        assertEquals("banana", val.getValue());
    }

    @Test
    public void testKeyExpirationWithTTL() throws InterruptedException {
        store.set("tempKey", "tempVal", 100L);

        Value activeVal = store.get("tempKey");
        assertNotNull(activeVal);
        assertEquals("tempVal", activeVal.getValue());

        Thread.sleep(150);

        Value expiredVal = store.get("tempKey");
        assertNull(expiredVal);
    }
}
