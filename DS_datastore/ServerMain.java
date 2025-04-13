package DS_datastore;

import java.util.*;

/**
 * Entry point for launching a server in the distributed key-value store system.
 * <p>
 * This class parses command-line arguments, assigns ports based on the server ID,
 * initializes the server configuration, and starts the server.
 * <p>
 * Usage:
 * <pre>
 *     java DS_datastore.ServerMain &lt;serverId&gt; [&lt;seedHost&gt; &lt;seedDiscoveryPort&gt;]
 * </pre>
 * Example:
 * <pre>
 *     java DS_datastore.ServerMain server2 192.168.1.100 8090
 * </pre>
 *
 * Available server IDs: {@code server1}, {@code server2}, {@code server3}
 */
public class ServerMain {

    /**
     * Main method that starts the server.
     * <p>
     * It expects at least one argument:
     * <ul>
     *     <li>{@code serverId}: one of {@code server1}, {@code server2}, or {@code server3}</li>
     *     <li>{@code seedHost} (optional): the IP/host of a known server to join the network</li>
     *     <li>{@code seedDiscoveryPort} (optional): the discovery port of the seed server</li>
     * </ul>
     *
     * @param args command-line arguments to configure the server
     */
    public static void main(String[] args) {
        // Usage: java DS_datastore.ServerMain <serverId> [<seedHost> <seedDiscoveryPort>]
        if (args.length < 1) {
            System.out.println("Usage: java DS_datastore.ServerMain <serverId> [<seedHost> <seedDiscoveryPort>]");
            System.exit(1);
        }
        String serverId = args[0];
        String seedHost = (args.length >= 2) ? args[1] : "";
        int seedDiscoveryPort = (args.length >= 3) ? Integer.parseInt(args[2]) : 0;

        // All server IDs in the system (for vector clocks)
        Set<String> initialServerIds = new HashSet<>();
        initialServerIds.add(serverId);


        // Peer servers list starts empty; discovery will fill it.
        List<PeerInfo> peerServers = new ArrayList<>();

        // Configure ports for this server.
        int clientPort, replicationPort, discoveryPort, stateTransferPort;
        if ("server1".equals(serverId)) {
            clientPort = 8080;
            replicationPort = 8085;
            discoveryPort = 8090;
            stateTransferPort = 9085;
        } else if ("server2".equals(serverId)) {
            clientPort = 8081;
            replicationPort = 8086;
            discoveryPort = 8091;
            stateTransferPort = 9091;
        } else if ("server3".equals(serverId)) {
            clientPort = 8082;
            replicationPort = 8087;
            discoveryPort = 8092;
            stateTransferPort = 9092;
        } else {
            System.out.println("Unknown serverId. Use server1, server2, or server3.");
            return;
        }

        // Instantiate and start the server
        Server server = new Server(serverId, clientPort, replicationPort, discoveryPort, stateTransferPort,
                initialServerIds, peerServers, seedHost, seedDiscoveryPort);
        server.start();
    }
}
