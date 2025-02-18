package DS_datastore;

import java.net.*;
import java.io.*;

public class ReplicationListener implements Runnable {
    private int port;
    private Server server;

    public ReplicationListener(int port, Server server) {
        this.port = port;
        this.server = server;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Replication listener started on port " + port);
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
