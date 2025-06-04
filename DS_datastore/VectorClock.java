package DS_datastore;

import java.io.Serializable;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a vector clock used to track causal relationships between events
 * in a distributed system.
 * <p>
 * Each server maintains a vector clock, where each entry corresponds to the latest
 * known logical time of all participating servers. This enables detection of
 * causality and concurrent updates.
 * <p>
 * Implements {@link Serializable} to allow transmission across the network.
 */
public class VectorClock implements Serializable {
    /** The internal map representing logical timestamps for each server. */
    private Map<String, Integer> clock;

    /**
     * Constructs a new vector clock initialized with zero for each server ID.
     *
     * @param serverIds the set of all participating server IDs
     */
    public VectorClock(Set<String> serverIds) {
        clock = new HashMap<>();
        for (String id : serverIds) {
            clock.put(id, 0);
        }
    }

    /**
     * Adds a new server ID to the clock, initializing its counter to zero,
     * only if it's not already present.
     *
     * @param serverId the new server ID to add
     */
    public synchronized void addServer(String serverId) {
        clock.putIfAbsent(serverId, 0);
    }

    /**
     * Constructs a deep copy of another vector clock.
     *
     * @param other the vector clock to copy
     */
    public VectorClock(VectorClock other) {
        clock = new HashMap<>(other.clock);
    }

    /**
     * Increments the logical time for the given server ID by one.
     *
     * @param serverId the server performing an update
     */
    public synchronized void increment(String serverId) {
        clock.put(serverId, clock.get(serverId) + 1);
    }

    /**
     * Returns a copy of the current vector clock map.
     *
     * @return a new map representing the vector clock state
     */
    public synchronized Map<String, Integer> getClock() {
        return new HashMap<>(clock);
    }

    /**
     * Merges this vector clock with another one by taking the element-wise maximum.
     * <p>
     * Useful when incorporating updates from another server.
     *
     * @param other the vector clock to merge from
     */
    public synchronized void merge(VectorClock other) {
        for (String id : other.clock.keySet()) {
            int otherTime = other.clock.get(id);
            int thisTime = clock.getOrDefault(id, 0);
            clock.put(id, Math.max(thisTime, otherTime));
        }
    }

    /**
     * Checks whether an update with the given vector clock is causally ready to be applied.
     * <p>
     * This method enforces causal consistency by checking two conditions:
     * <ul>
     *   <li><b>For all other servers</b> (i.e., servers other than the one that sent the update),
     *       the local clock must be at least as up to date as the update's clock:
     *       {@code localClock[j] ≥ updateClock[j]}</li>
     *   <li><b>For the server that sent the update</b> ({@code srcId}), the local clock must
     *       be exactly one step behind:
     *       {@code localClock[srcId] == updateClock[srcId] - 1}</li>
     * </ul>
     * In other words, the update is only applied if all dependencies are already applied,
     * and this is the next expected event from {@code srcId}.
     *
     * @param srcId       the ID of the server that generated the update
     * @param updateClock the vector clock timestamp of the incoming update
     * @return {@code true} if the update respects causal ordering and can be applied;
     *         {@code false} otherwise
     */
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

    /**
     * Returns true if *this* VC is ≥ another one for **all** components.
     * Useful to discharge updates already included in the snapshot. Utile per scartare update ormai inclusi nello snapshot.
     */
    public synchronized boolean dominates(VectorClock other) {
        for (String id : other.clock.keySet()) {
            int thisTime   = clock.getOrDefault(id, 0);
            int otherTime  = other.clock.get(id);
            if (thisTime < otherTime) {
                return false;        // there is at least an older component.
            }
        }
        return true;                 // all components >=
    }


    /**
     * Returns a string representation of the vector clock.
     *
     * @return the string form of the internal clock map
     */
    @Override
    public String toString() {
        return clock.toString();
    }
}
