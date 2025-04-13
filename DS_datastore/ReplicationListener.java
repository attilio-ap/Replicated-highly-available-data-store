package DS_datastore;

import java.net.*;
import java.io.*;

/**
 * Listens for incoming replication messages from other peer servers in the system.
 * <p>
 * This class runs on its own thread and listens on the server's replication port.
 * For each incoming connection, it spawns a new thread to handle incoming {@link UpdateMessage}
 * objects and delegates them to the server's update handler.
 * <p>
 * Used to maintain eventual consistency between nodes in a distributed key-value store.
 */
public class ReplicationListener implements Runnable {
    /** Reference to the main server instance that processes remote updates. */
    private Server server;

    /**
     * Constructs a {@code ReplicationListener} for the specified server.
     *
     * @param server the server instance that will handle replication messages
     */
    public ReplicationListener(Server server) {
        this.server = server;
    }

    /**
     * Starts listening on the server's replication port.
     * <p>
     * For each incoming connection, this method spawns a new thread to:
     * <ul>
     *     <li>Receive a serialized {@link UpdateMessage}</li>
     *     <li>Pass it to {@link Server#handleRemoteUpdate(UpdateMessage)}</li>
     * </ul>
     * If any error occurs during deserialization or processing, it is logged to standard error.
     */
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(server.getReplicationPort())) {
            System.out.println("Replication listener started on port " + server.getReplicationPort());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                        Object obj = in.readObject();
                        if (obj instanceof UpdateMessage) {
                            UpdateMessage update = (UpdateMessage) obj;
                            server.handleRemoteUpdate(update);
                        }
                    } catch (Exception e) {
                        System.err.println("Replication message error: " + e.getMessage());
                    } finally {
                        try { socket.close(); } catch (IOException e) { }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("ReplicationListener error: " + e.getMessage());
        }
    }
}
