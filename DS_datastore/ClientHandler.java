package DS_datastore;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String request = in.readLine();
            if (request != null) {
                // Splitta la richiesta in token. Il comando SHOW non necessita di ulteriori argomenti.
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
                    // Gestione del comando SHOW: restituisce tutto il contenuto del KeyValueStore.
                    Map<String, ValueEntry> snapshot = server.getKeyValueStoreSnapshot();
                    if (snapshot.isEmpty()) {
                        out.println("Store is empty.");
                    } else {
                        for (Map.Entry<String, ValueEntry> entry : snapshot.entrySet()) {
                            out.println(entry.getKey() + " => " + entry.getValue().toString());
                        }
                    }
                    // Indica la fine della risposta.
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
            } catch (IOException e) { }
        }
    }
}
