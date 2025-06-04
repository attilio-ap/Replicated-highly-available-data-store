package DS_datastore;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Handles discovery-related communication between peer servers in a distributed system.
 * <p>
 * This listener accepts discovery messages (such as JOIN_REQUEST or NEW_PEER),
 * processes them accordingly, and replies with appropriate peer information.
 * <p>
 * Intended to be run on a dedicated thread via the {@link Runnable} interface.
 */
public class DiscoveryListener implements Runnable {
    /** Reference to the main server instance for accessing peer management and ports. */
    private Server server;

    /**
     * Determines the correct local IPv4 address of the machine.
     * <p>
     * It filters out loopback, virtual, and inactive interfaces, and searches for
     * an address starting with "192.168.1.", which is commonly used in local networks.
     *
     * @return the local IP address as a string, or "IP not founded" if none is found
     * @throws SocketException if an I/O error occurs while querying the network interfaces
     */
    public static String getCorrectIP() throws SocketException {
        return java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces()).stream()
                .filter(i -> {
                    try {
                        return i.isUp() && !i.isLoopback() && !i.isVirtual();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .flatMap(i -> java.util.Collections.list(i.getInetAddresses()).stream())
                .filter(addr -> addr instanceof java.net.Inet4Address && addr.getHostAddress().startsWith("192.168.1."))
                .map(java.net.InetAddress::getHostAddress)
                .findFirst()
                .orElse("IP not founded");
    }

    /**
     * Constructs a new DiscoveryListener.
     *
     * @param server the server instance managing the distributed peer system
     */
    public DiscoveryListener( Server server) {
        this.server = server;
    }

    /**
     * Starts the discovery listener on the server's discovery port.
     * <p>
     * For each incoming discovery connection, a new thread is spawned to handle:
     * <ul>
     *     <li><b>JOIN_REQUEST</b>: adds the requesting peer and replies with the current peer list (excluding the requester).</li>
     *     <li><b>NEW_PEER</b>: registers a newly joined peer across the system.</li>
     * </ul>
     * If any other type of message is received or if an error occurs, it is logged.
     */
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(server.getDiscoveryPort())) {
            System.out.println("Discovery listener started on port " + server.getDiscoveryPort());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        Object obj = in.readObject();
                        if (obj instanceof DiscoveryMessage) {
                            DiscoveryMessage msg = (DiscoveryMessage) obj;
                            if (msg.getType() == DiscoveryMessage.Type.JOIN_REQUEST) {
                                // Create the PeerInfo for the new node using the received serverId, host, and replicationPort.
                                PeerInfo newPeer = new PeerInfo(msg.getServerId(), msg.getHost(), msg.getReplicationPort(), msg.getDiscoveryPort(), msg.getStateTransferPort());
                                server.addPeer(newPeer);
                                server.getLocalClock().addServer(newPeer.getServerId());

                                // Prepare the list of peers to include in the response.
                                // Include the information of the server receiving the request (i.e., self) and all other known peers,
                                // excluding the node that sent the JOIN_REQUEST.

                                List<PeerInfo> responseList = new ArrayList<>();

                                // Get the local host.
                                String selfHost = getCorrectIP();
                                // Create the PeerInfo for the current server (self).
                                PeerInfo selfPeer = new PeerInfo(server.getServerId(), selfHost, server.getReplicationPort(), server.getDiscoveryPort(), server.getStateTransferPort());
                                responseList.add(selfPeer);

                                // Add the other peers (exclude the node that sent the request to avoid duplicates).
                                for (PeerInfo p : server.getPeerServers()) {
                                    if (!p.getServerId().equals(msg.getServerId())) {
                                        responseList.add(p);
                                    }
                                }

                                // Send the response containing the list of peers.
                                DiscoveryMessage response = new DiscoveryMessage(DiscoveryMessage.Type.JOIN_RESPONSE, responseList);
                                out.writeObject(response);
                                out.flush();
                                System.out.println("Processed JOIN_REQUEST from " + msg.getServerId());
                            }
                            if (msg.getType() == DiscoveryMessage.Type.NEW_PEER) {
                                PeerInfo newPeer = msg.getNewPeer();
                                // Add only if not present
                                server.addPeer(newPeer);
                                server.getLocalClock().addServer(newPeer.getServerId());
                                System.out.println("Ricevuto NEW_PEER: " + newPeer);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Discovery message error: " + e.getMessage());
                    } finally {
                        try { socket.close(); } catch (IOException e) { }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Discovery listener error: " + e.getMessage());
        }
    }
}
