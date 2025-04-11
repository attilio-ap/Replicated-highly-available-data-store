package DS_datastore;

import java.net.*;
import java.io.*;

public class ReplicationListener implements Runnable {
    private Server server;

    public ReplicationListener(Server server) {
        this.server = server;
    }

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
