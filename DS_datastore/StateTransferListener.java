package DS_datastore;

import java.net.*;
import java.io.*;

public class StateTransferListener implements Runnable {
    private int stateTransferPort;
    private Server server;

    public StateTransferListener(int stateTransferPort, Server server) {
        this.stateTransferPort = stateTransferPort;
        this.server = server;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(stateTransferPort)) {
            System.out.println("StateTransfer listener started on port " + stateTransferPort);
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
