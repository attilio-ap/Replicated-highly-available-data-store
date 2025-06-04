package DS_datastore;

import java.io.Serializable;

/**
 * Marker interface for messages that can be replicated or transmitted
 * between nodes in a distributed system.
 * <p>
 * This interface extends {@link Serializable} to ensure that all implementing
 * classes can be serialized for network communication or persistence.
 * <p>
 * It can be expanded with common methods that all replicable messages should implement,
 * if needed in the future.
 */
public interface ReplicableMessage extends Serializable {

}
