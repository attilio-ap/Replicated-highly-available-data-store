package DS_datastore;

import java.io.Serializable;

/**
 * Represents the basic connection information of a peer server
 * in a distributed system. This includes identifiers and ports
 * used for replication, discovery, and state transfer.
 * <p>
 * Implements {@link Serializable} so that it can be easily transmitted
 * over a network or stored persistently.
 */
public class PeerInfo implements Serializable {
    /** Unique identifier of the peer server. */
    private String serverId;

    /** Hostname or IP address of the peer server. */
    private String host;

    /** Port used for replication communication. */
    private int replicationPort;

    /** Port used for discovery services (e.g., join requests). */
    private int discoveryPort;

    /** Port used for transferring state (e.g., during recovery or synchronization). */
    private int stateTransferPort;

    /**
     * Constructs a new {@code PeerInfo} instance with all required connection details.
     *
     * @param serverId           the unique identifier of the peer
     * @param host               the hostname or IP address of the peer
     * @param replicationPort    the port used for replication
     * @param discoveryPort      the port used for discovery messages
     * @param stateTransferPort  the port used for state transfer operations
     */
    public PeerInfo(String serverId, String host, int replicationPort, int discoveryPort, int stateTransferPort) {
        this.serverId = serverId;
        this.host = host;
        this.replicationPort = replicationPort;
        this.discoveryPort = discoveryPort;
        this.stateTransferPort = stateTransferPort;
    }

    /**
     * Returns the unique identifier of the peer.
     *
     * @return the server ID
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Returns the host address of the peer.
     *
     * @return the host (IP or hostname)
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port used for replication communication.
     *
     * @return the replication port
     */
    public int getReplicationPort() {
        return replicationPort;
    }

    /**
     * Returns the port used for discovery messages.
     *
     * @return the discovery port
     */
    public int getStateTransferPort(){
        return stateTransferPort;
    }

    /**
     * Returns the port used for state transfer.
     *
     * @return the state transfer port
     */
    public int getDiscoveryPort() {
        return discoveryPort;
    }

    /**
     * Returns a string representation of the peer in the format:
     * {@code serverId@host:replicationPort}.
     *
     * @return a human-readable string representing the peer
     */
    @Override
    public String toString() {
        return serverId + "@" + host + ":" + replicationPort;
    }
}

