package DS_datastore;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Handles communication with a single client connected to the server.
 * This class processes commands such as READ, WRITE, and SHOW from the client,
 * interacts with the server's key-value store accordingly, and sends responses back.
 *
 * Implements {@link Runnable} to allow execution in a separate thread.
 */
public class ClientHandler implements Runnable {
    /** The socket associated with the connected client. */
    private Socket clientSocket;

    /** Reference to the main server instance to access shared data and operations. */
    private Server server;

    /**
     * Constructs a new ClientHandler.
     *
     * @param socket the socket representing the client's connection
     * @param server the server instance managing the key-value store
     */
    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
    }

    /**
     * Handles the client's request.
     * <p>
     * Supported commands:
     * <ul>
     *     <li><b>READ key</b>: returns the value associated with the given key.</li>
     *     <li><b>WRITE key value</b>: stores or updates the value associated with the key.</li>
     *     <li><b>SHOW</b>: returns the entire contents of the key-value store.</li>
     * </ul>
     * Any unknown or malformed commands will return an error message.
     *
     * This method runs in its own thread when executed by a thread executor or manually started.
     */
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String request = in.readLine();
            if (request != null) {
                // Split the request in token. SHOW command doesn't need other arguments.
                String[] tokens = request.split(" ", 3);
                String command = tokens[0].toUpperCase();

                if ("READ".equals(command)) {
                    String key = tokens[1];
                    String value = server.handleLocalRead(key);
                    if (value != null) {
                        out.println("Key: " + key + "; Value: " + value);
                    } else {
                        out.println("ERROR: Key not found");
                    }
                } else if ("WRITE".equals(command)) {
                    if (tokens.length < 3) {
                        out.println("ERROR: Invalid WRITE command. Usage: WRITE key value");
                    } else {
                        String key = tokens[1];
                        String value = tokens[2];
                        server.handleLocalWrite(key, value);
                        out.println("Write successful");
                    }
                } else if ("SHOW".equals(command)) {
                    // SHOW command handle: returns KeyValueStore contents.
                    Map<String, ValueEntry> snapshot = server.getKeyValueStoreSnapshot();
                    if (snapshot.isEmpty()) {
                        out.println("Store is empty.");
                    } else {
                        for (Map.Entry<String, ValueEntry> entry : snapshot.entrySet()) {
                            out.println(entry.getKey() + " => " + entry.getValue().toString());
                        }
                    }
                    // Indicates response' end.
                    out.println("END_OF_SHOW");
                } else {
                    out.println("ERROR: Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore closing error
            }
        }
    }
}
