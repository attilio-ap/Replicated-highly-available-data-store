package DS_datastore;

import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Background thread that continuously retries sending failed replication or discovery messages
 * to peer servers in a distributed system.
 * <p>
 * It periodically (every 2 seconds) scans the pending replication queue from the server
 * and attempts to re-send messages. If the delivery is successful, the message is removed
 * from the pending list. This helps maintain eventual consistency and robustness in case
 * of temporary network issues.
 */
public class ReplicationRetryThread implements Runnable {
    /** Reference to the server instance managing the pending replication messages. */
    private Server server;

    /**
     * Constructs a {@code ReplicationRetryThread} with the specified server.
     *
     * @param server the server instance whose pending replication messages will be processed
     */
    public ReplicationRetryThread(Server server) {
        this.server = server;
    }

    /**
     * Continuously runs the retry loop:
     * <ul>
     *     <li>Sleeps for 2 seconds between each retry cycle</li>
     *     <li>Iterates through the map of pending replication messages</li>
     *     <li>Tries to send each message to its corresponding peer</li>
     *     <li>Removes successfully sent messages from the queue</li>
     * </ul>
     * If a thread interruption occurs, the method exits gracefully.
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000); // Ritenta ogni 2 secondi
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Map<PeerInfo, List<ReplicableMessage>> pendingMap = server.getPendingReplications();
            for (PeerInfo peer : pendingMap.keySet()) {
                // Copy of the list to avoid ConcurrentModificationException
                List<ReplicableMessage> messages = new ArrayList<>(pendingMap.get(peer));
                for (ReplicableMessage msg : messages) {
                    // Determin destination port;
                    int destPort = (msg instanceof DiscoveryMessage) ? peer.getDiscoveryPort() : peer.getReplicationPort();
                    try (Socket socket = new Socket(peer.getHost(), destPort);
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        out.writeObject(msg);
                        out.flush();
                        // If sent correctly, remove the message from the queue
                        pendingMap.get(peer).remove(msg);
                        System.out.println("Successfully resent message to " + peer.getHost() + ":" + destPort);
                    } catch (Exception e) {
                        System.err.println("Retry sending to " + peer.getHost() + ":" + destPort + " failed: " + e.getMessage());
                    }
                }
            }
        }
    }
}
