package DS_datastore;

import java.io.Serializable;
import java.util.Map;

public class StateResponseMessage implements Serializable {
    private Map<String, ValueEntry> storeSnapshot;
    private VectorClock vectorClock;

    public StateResponseMessage(Map<String, ValueEntry> storeSnapshot, VectorClock vectorClock) {
        this.storeSnapshot = storeSnapshot;
        this.vectorClock = new VectorClock(vectorClock);
    }

    public Map<String, ValueEntry> getStoreSnapshot() {
        return storeSnapshot;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }
}
