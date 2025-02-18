package DS_datastore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class KeyValueStore {
    private final Map<String, ValueEntry> store = new ConcurrentHashMap<>();

    public synchronized void write(String key, String value, VectorClock vc) {
        store.put(key, new ValueEntry(value, vc));
    }

    public synchronized String read(String key) {
        ValueEntry entry = store.get(key);
        return (entry != null) ? entry.getValue() : null;
    }

    public synchronized Map<String, ValueEntry> getStoreSnapshot() {
        return new ConcurrentHashMap<>(store);
    }
}
