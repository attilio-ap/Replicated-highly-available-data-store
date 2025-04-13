package DS_datastore;

import java.net.*;
import java.io.*;

/**
 * A listener that handles incoming state transfer requests from peer servers.
 * <p>
 * This component allows a server to respond to {@link StateRequestMessage}s
 * by sending a snapshot of its key-value store and vector clock, wrapped in a
 * {@link StateResponseMessage}. It runs on its own thread and listens on the
 * server's state transfer port.
 */
public class StateTransferListener implements Runnable {
    /** Reference to the server instance that provides the state data. */
    private Server server;

    /**
     * Constructs a new {@code StateTransferListener} for the given server.
     *
     * @param server the server instance that will provide state information to peers
     */
    public StateTransferListener(Server server) {
        this.server = server;
    }

    /**
     * Starts the state transfer listener. It accepts incoming socket connections on
     * the state transfer port, reads {@link StateRequestMessage} objects, and replies
     * with the server's current state.
     * <p>
     * Each connection is handled in a separate thread for scalability.
     */
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(server.getStateTransferPort())) {
            System.out.println("StateTransfer listener started on port " + server.getStateTransferPort());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        Object obj = in.readObject();
                        if (obj instanceof StateRequestMessage) {
                            // Prepare and send the state response message.
                            StateResponseMessage response = new StateResponseMessage(
                                    server.getKeyValueStoreSnapshot(),
                                    server.getLocalClock()
                            );
                            out.writeObject(response);
                            out.flush();
                            System.out.println("State transferred to requesting peer.");
                        }
                    } catch (Exception e) {
                        System.err.println("StateTransfer error: " + e.getMessage());
                    } finally {
                        try { socket.close(); } catch (IOException e) { }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("StateTransferListener error: " + e.getMessage());
        }
    }
}
