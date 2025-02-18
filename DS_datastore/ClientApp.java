package DS_datastore;

import java.io.*;
import java.net.*;

public class ClientApp {
    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: java ClientApp <server-host> <server-port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server " + host + ":" + port);
            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
                String response = in.readLine();
                System.out.println("Response: " + response);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

