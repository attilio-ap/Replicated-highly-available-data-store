package DS_datastore;

import java.io.Serializable;

/**
 * A message representing a key-value update, along with its causal metadata,
 * to be replicated across peer servers in the distributed system.
 * <p>
 * This class implements {@link ReplicableMessage} and can be sent over the network
 * during replication or stored for retry upon failure.
 */
public class UpdateMessage implements ReplicableMessage {
    /** The key being updated. */
    private String key;

    /** The new value to associate with the key. */
    private String value;

    /** The ID of the server that originated the update. */
    private String originServerId;

    /** The vector clock representing the causal timestamp of this update. */
    private VectorClock vectorClock;

    /**
     * Constructs a new {@code UpdateMessage}.
     *
     * @param key            the key to update
     * @param value          the new value
     * @param originServerId the ID of the server that generated the update
     * @param vectorClock    the vector clock representing the causal context of the update
     */
    public UpdateMessage(String key, String value, String originServerId, VectorClock vectorClock) {
        this.key = key;
        this.value = value;
        this.originServerId = originServerId;
        this.vectorClock = new VectorClock(vectorClock); // copy the VC
    }

    /**
     * Returns the key associated with the update.
     *
     * @return the updated key
     */
    public String getKey() { return key; }

    /**
     * Returns the value to associate with the key.
     *
     * @return the new value
     */
    public String getValue() { return value; }

    /**
     * Returns the ID of the server that created the update.
     *
     * @return the origin server ID
     */
    public String getOriginServerId() { return originServerId; }

    /**
     * Returns the vector clock representing the causal metadata of the update.
     *
     * @return the update's vector clock
     */
    public VectorClock getVectorClock() { return new VectorClock(vectorClock); }
}
