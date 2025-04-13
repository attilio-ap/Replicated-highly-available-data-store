package DS_datastore;

import java.io.Serializable;

/**
 * Represents a value stored in the key-value store along with its associated
 * vector clock for tracking causal history in a distributed system.
 * <p>
 * This class is used to maintain both the actual data and its version metadata,
 * allowing consistent conflict resolution and ordering of updates.
 * <p>
 * Implements {@link Serializable} for transmission or persistent storage.
 */
public class ValueEntry implements Serializable {
    /** The actual value stored under a key. */
    private String value;

    /** The vector clock associated with this value. */
    private VectorClock vClock;

    /**
     * Constructs a new {@code ValueEntry} with the given value and vector clock.
     *
     * @param value  the value to store
     * @param vClock the vector clock representing the version of the value
     */
    public ValueEntry(String value, VectorClock vClock) {
        this.value = value;
        this.vClock = vClock; // make a copy
    }

    /**
     * Returns the stored value.
     *
     * @return the value string
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the vector clock associated with the value.
     *
     * @return the vector clock
     */
    public VectorClock getVectorClock() {
        return vClock;
    }

    /**
     * Returns a human-readable string representation of the entry,
     * including its value and vector clock.
     *
     * @return a string describing the value and its vector clock
     */
    @Override
    public String toString() {
        return "Value: " + value + ", VC: " + vClock.toString();
    }
}
