package DS_datastore;

import java.net.*;
import java.io.*;

/**
 * Listens for incoming client connections on a specified port and
 * spawns a new {@link ClientHandler} thread for each connected client.
 *
 * This class is meant to be executed in a separate thread or thread pool
 * by implementing the {@link Runnable} interface.
 */
public class ClientListener implements Runnable {
    /** The port number on which the listener will accept client connections. */
    private int port;

    /** Reference to the main server instance that handles client requests. */
    private Server server;

    /**
     * Constructs a new ClientListener.
     *
     * @param port   the port on which to listen for client connections
     * @param server the server instance that will handle client interactions
     */
    public ClientListener(int port, Server server) {
        this.port = port;
        this.server = server;
    }

    /**
     * Starts the listener on the specified port.
     * <p>
     * For each incoming client connection, a new {@link ClientHandler} is created
     * and executed in its own thread to handle the client's request.
     */
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
