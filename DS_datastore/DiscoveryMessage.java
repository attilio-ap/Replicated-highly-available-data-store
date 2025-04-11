package DS_datastore;

import java.io.Serializable;
import java.util.List;

public class DiscoveryMessage implements ReplicableMessage {
    public enum Type {
        JOIN_REQUEST,
        JOIN_RESPONSE,
        NEW_PEER  // Nuovo tipo per notificare l'arrivo di un nuovo peer
    }

    private Type type;
    private String serverId; // Usato per JOIN_REQUEST
    private String host;     // Usato per JOIN_REQUEST
    private int replicationPort; // Usato per JOIN_REQUEST
    private int discoveryPort;
    private int stateTransferPort;
    private List<PeerInfo> peerList; // Usato per JOIN_RESPONSE
    private PeerInfo newPeer; // Usato per NEW_PEER

    // Costruttore per JOIN_REQUEST
    public DiscoveryMessage(Type type, String serverId, String host, int replicationPort, int discoveryPort, int stateTransferPort) {
        this.type = type;
        this.serverId = serverId;
        this.host = host;
        this.replicationPort = replicationPort;
        this.discoveryPort = discoveryPort;
        this.stateTransferPort = stateTransferPort;
        // Il clientPort può essere usato se necessario
    }

    // Costruttore per JOIN_RESPONSE
    public DiscoveryMessage(Type type, List<PeerInfo> peerList) {
        this.type = type;
        this.peerList = peerList;
    }

    // Costruttore per NEW_PEER
    public DiscoveryMessage(Type type, PeerInfo newPeer) {
        this.type = type;
        this.newPeer = newPeer;
    }

    // Getter e setter
    public Type getType() {
        return type;
    }

    public String getServerId() {
        return serverId;
    }

    public String getHost() {
        return host;
    }

    public int getReplicationPort() {
        return replicationPort;
    }
    public int getStateTransferPort() {
        return stateTransferPort;
    }
    public int getDiscoveryPort() {
        return discoveryPort;
    }

    public List<PeerInfo> getPeerList() {
        return peerList;
    }

    public PeerInfo getNewPeer() {
        return newPeer;
    }
}
