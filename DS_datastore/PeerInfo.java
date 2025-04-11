package DS_datastore;

import java.io.Serializable;

public class PeerInfo implements Serializable {
    private String serverId;
    private String host;
    private int replicationPort;
    private int discoveryPort;
    private int stateTransferPort;

    public PeerInfo(String serverId, String host, int replicationPort, int discoveryPort, int stateTransferPort) {
        this.serverId = serverId;
        this.host = host;
        this.replicationPort = replicationPort;
        this.discoveryPort = discoveryPort;
        this.stateTransferPort = stateTransferPort;
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

    public int getStateTransferPort(){
        return stateTransferPort;
    }

    public int getDiscoveryPort() {
        return discoveryPort;
    }

    @Override
    public String toString() {
        return serverId + "@" + host + ":" + replicationPort;
    }
}

