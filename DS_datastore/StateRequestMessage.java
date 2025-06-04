package DS_datastore;

import java.io.Serializable;

/**
 * A marker message used to request the full state (key-value store and vector clock)
 * from a peer server in the distributed system.
 * <p>
 * This message is sent during the recovery process to initiate state transfer.
 * <p>
 * It implements {@link Serializable} to allow transmission over network sockets.
 */
public class StateRequestMessage implements Serializable {
    // Empty class used as a signal for state request.
}

