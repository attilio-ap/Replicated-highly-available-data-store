package DS_datastore;

import java.io.Serializable;
import java.util.Map;

/**
 * A message sent in response to a {@link StateRequestMessage}, containing the
 * full current state of the server's key-value store and its associated vector clock.
 * <p>
 * Used during state recovery by newly joining or recovering servers in the system.
 * This message must be serializable to be sent over a network socket.
 */
public class StateResponseMessage implements Serializable {

    /** Snapshot of the current key-value store. */
    private Map<String, ValueEntry> storeSnapshot;

    /** Current vector clock of the server sending the state. */
    private VectorClock vectorClock;

    /**
     * Constructs a new {@code StateResponseMessage} with the given store snapshot and vector clock.
     *
     * @param storeSnapshot the key-value pairs representing the current store state
     * @param vectorClock   the current vector clock of the server
     */
    public StateResponseMessage(Map<String, ValueEntry> storeSnapshot, VectorClock vectorClock) {
        this.storeSnapshot = storeSnapshot;
        this.vectorClock = new VectorClock(vectorClock);
    }

    /**
     * Returns the key-value store snapshot sent in the response.
     *
     * @return a map containing the current store data
     */
    public Map<String, ValueEntry> getStoreSnapshot() {
        return storeSnapshot;
    }

    /**
     * Returns the vector clock representing the version state of the server.
     *
     * @return the vector clock at the time of snapshot
     */
    public VectorClock getVectorClock() {
        return vectorClock;
    }
}
