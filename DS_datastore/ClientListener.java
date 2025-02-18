package DS_datastore;

import java.net.*;
import java.io.*;

public class ClientListener implements Runnable {
    private int port;
    private Server server;

    public ClientListener(int port, Server server) {
        this.port = port;
        this.server = server;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Client listener started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, server)).start();
            }
        } catch (IOException e) {
            System.err.println("ClientListener error: " + e.getMessage());
        }
    }
}
