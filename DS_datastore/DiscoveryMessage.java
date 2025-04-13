package DS_datastore;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a message used in the peer discovery protocol between servers
 * in a distributed system. This message can serve different purposes such as
 * initiating a join request, responding to it, or broadcasting the arrival of a new peer.
 * <p>
 * Implements {@link ReplicableMessage} to ensure it can be transmitted between peers.
 */
public class DiscoveryMessage implements ReplicableMessage {

    /**
     * Defines the type of the discovery message.
     */
    public enum Type {
        /** Sent by a node that wants to join the system. */
        JOIN_REQUEST,
        /** Sent in response to a JOIN_REQUEST, containing known peers. */
        JOIN_RESPONSE,
        /** Broadcast to notify the network of a new peer. */
        NEW_PEER
    }

    private Type type;
    private String serverId; // Used for JOIN_REQUEST
    private String host;     // Used for JOIN_REQUEST
    private int replicationPort; // Used for JOIN_REQUEST
    private int discoveryPort;
    private int stateTransferPort;
    private List<PeerInfo> peerList; // Used for JOIN_RESPONSE
    private PeerInfo newPeer; // Used for NEW_PEER

    /**
     * Constructs a {@code DiscoveryMessage} for a JOIN_REQUEST.
     *
     * @param type               the message type (should be {@code Type.JOIN_REQUEST})
     * @param serverId           the ID of the server requesting to join
     * @param host               the host of the requesting server
     * @param replicationPort    the replication port of the requesting server
     * @param discoveryPort      the discovery port of the requesting server
     * @param stateTransferPort  the state transfer port of the requesting server
     */
    public DiscoveryMessage(Type type, String serverId, String host, int replicationPort, int discoveryPort, int stateTransferPort) {
        this.type = type;
        this.serverId = serverId;
        this.host = host;
        this.replicationPort = replicationPort;
        this.discoveryPort = discoveryPort;
        this.stateTransferPort = stateTransferPort;
        // Il clientPort può essere usato se necessario
    }

    /**
     * Constructs a {@code DiscoveryMessage} for a JOIN_RESPONSE.
     *
     * @param type      the message type (should be {@code Type.JOIN_RESPONSE})
     * @param peerList  the list of known peers to return to the requester
     */
    public DiscoveryMessage(Type type, List<PeerInfo> peerList) {
        this.type = type;
        this.peerList = peerList;
    }

    /**
     * Constructs a {@code DiscoveryMessage} for broadcasting a NEW_PEER.
     *
     * @param type     the message type (should be {@code Type.NEW_PEER})
     * @param newPeer  the information of the newly joined peer
     */
    public DiscoveryMessage(Type type, PeerInfo newPeer) {
        this.type = type;
        this.newPeer = newPeer;
    }

    /**
     * Returns the type of the discovery message.
     *
     * @return the message type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the server ID (used for JOIN_REQUEST).
     *
     * @return the server ID
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Returns the host of the requesting server (used for JOIN_REQUEST).
     *
     * @return the host address
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the replication port of the requesting server (used for JOIN_REQUEST).
     *
     * @return the replication port
     */
    public int getReplicationPort() {
        return replicationPort;
    }

    /**
     * Returns the discovery port of the requesting server (used for JOIN_REQUEST).
     *
     * @return the discovery port
     */
    public int getStateTransferPort() {
        return stateTransferPort;
    }

    /**
     * Returns the state transfer port of the requesting server (used for JOIN_REQUEST).
     *
     * @return the state transfer port
     */
    public int getDiscoveryPort() {
        return discoveryPort;
    }

    /**
     * Returns the list of peers (used for JOIN_RESPONSE).
     *
     * @return the list of known peers
     */
    public List<PeerInfo> getPeerList() {
        return peerList;
    }

    /**
     * Returns the information about the new peer (used for NEW_PEER).
     *
     * @return the new peer
     */
    public PeerInfo getNewPeer() {
        return newPeer;
    }
}
