package DS_datastore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe key-value store that supports versioning through vector clocks.
 * <p>
 * Each key is associated with a {@link ValueEntry} containing the actual value
 * and its corresponding {@link VectorClock}.
 * The class provides synchronized methods for reading, writing, and retrieving a snapshot of the store.
 */
public class KeyValueStore {
    /**
     * Internal map that holds the key-value pairs, where each value is wrapped
     * in a {@link ValueEntry} object that includes version information.
     */
    private final Map<String, ValueEntry> store = new ConcurrentHashMap<>();

    /**
     * Writes a key-value pair into the store along with its vector clock version.
     * If the key already exists, the value is overwritten.
     *
     * @param key   the key to write
     * @param value the value to associate with the key
     * @param vc    the vector clock representing the version of the value
     */
    public synchronized void write(String key, String value, VectorClock vc) {
        store.put(key, new ValueEntry(value, vc));
    }

    /**
     * Reads the value associated with the given key.
     *
     * @param key the key to read from the store
     * @return the value if found; otherwise {@code null}
     */
    public synchronized String read(String key) {
        ValueEntry entry = store.get(key);
        return (entry != null) ? entry.getValue() : null;
    }

    /**
     * Returns a snapshot of the current state of the store.
     * <p>
     * The snapshot is a shallow copy of the internal map and reflects the values
     * and vector clocks at the time of calling.
     *
     * @return a copy of the current key-value store
     */
    public synchronized Map<String, ValueEntry> getStoreSnapshot() {
        return new ConcurrentHashMap<>(store);
    }
}
