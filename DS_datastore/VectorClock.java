package DS_datastore;

import java.io.Serializable;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock implements Serializable {
    private Map<String, Integer> clock;

    public VectorClock(Set<String> serverIds) {
        clock = new HashMap<>();
        for (String id : serverIds) {
            clock.put(id, 0);
        }
    }

    // Metodo per aggiungere un nuovo server se non è già presente
    public synchronized void addServer(String serverId) {
        clock.putIfAbsent(serverId, 0);
    }

    // Copy constructor
    public VectorClock(VectorClock other) {
        clock = new HashMap<>(other.clock);
    }

    public synchronized void increment(String serverId) {
        clock.put(serverId, clock.get(serverId) + 1);
    }

    public synchronized Map<String, Integer> getClock() {
        return new HashMap<>(clock);
    }

    // Merge: take the element-wise maximum.
    public synchronized void merge(VectorClock other) {
        for (String id : other.clock.keySet()) {
            int otherTime = other.clock.get(id);
            int thisTime = clock.getOrDefault(id, 0);
            clock.put(id, Math.max(thisTime, otherTime));
        }
    }

    // Check if the update (with its VC) is causally ready to be applied.
    // For an update coming from server 'srcId', it is ready if:
    //    For all j != srcId: localClock[j] >= updateClock[j]
    //    And localClock[srcId] == updateClock[srcId] - 1
    public synchronized boolean canApply(String srcId, VectorClock updateClock) {
        for (String id : updateClock.clock.keySet()) {
            int localTime = clock.getOrDefault(id, 0);
            int updateTime = updateClock.clock.get(id);
            if (id.equals(srcId)) {
                if (localTime != updateTime - 1)
                    return false;
            } else {
                if (localTime < updateTime)
                    return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return clock.toString();
    }
}
