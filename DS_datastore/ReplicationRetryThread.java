package DS_datastore;

import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ReplicationRetryThread implements Runnable {
    private Server server;

    public ReplicationRetryThread(Server server) {
        this.server = server;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(2000); // Ritenta ogni 2 secondi
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Map<PeerInfo, List<ReplicableMessage>> pendingMap = server.getPendingReplications();
            for (PeerInfo peer : pendingMap.keySet()) {
                // Creiamo una copia della lista per evitare ConcurrentModificationException
                List<ReplicableMessage> messages = new ArrayList<>(pendingMap.get(peer));
                for (ReplicableMessage msg : messages) {
                    // Determina la porta di destinazione; per DiscoveryMessage, potresti usare la stessa logica usata in broadcastMyPresence
                    int destPort = (msg instanceof DiscoveryMessage) ? peer.getDiscoveryPort() : peer.getReplicationPort();
                    try (Socket socket = new Socket(peer.getHost(), destPort);
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        out.writeObject(msg);
                        out.flush();
                        // Se l'invio ha successo, rimuovi il messaggio dalla coda
                        pendingMap.get(peer).remove(msg);
                        System.out.println("Successfully resent message to " + peer.getHost() + ":" + destPort);
                    } catch (Exception e) {
                        System.err.println("Retry sending to " + peer.getHost() + ":" + destPort + " failed: " + e.getMessage());
                    }
                }
            }
        }
    }
}
