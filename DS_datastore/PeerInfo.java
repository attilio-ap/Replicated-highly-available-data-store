package DS_datastore;

import java.io.Serializable;

public class PeerInfo implements Serializable {
    private String serverId;
    private String host;
    private int port;

    public PeerInfo(String serverId, String host, int port) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
    }

    public String getServerId() {
        return serverId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return serverId + "@" + host + ":" + port;
    }
}

