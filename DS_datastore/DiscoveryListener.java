package DS_datastore;

import java.net.*;
import java.io.*;
import java.util.*;

public class DiscoveryListener implements Runnable {
    private Server server;

    public DiscoveryListener( Server server) {
        this.server = server;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(server.getDiscoveryPort())) {
            System.out.println("Discovery listener started on port " + server.getDiscoveryPort());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        Object obj = in.readObject();
                        if (obj instanceof DiscoveryMessage) {
                            DiscoveryMessage msg = (DiscoveryMessage) obj;
                            if (msg.getType() == DiscoveryMessage.Type.JOIN_REQUEST) {
                                // Crea il PeerInfo per il nuovo nodo usando serverId, host e replicationPort ricevuti.
                                PeerInfo newPeer = new PeerInfo(msg.getServerId(), msg.getHost(), msg.getReplicationPort(), msg.getDiscoveryPort(), msg.getStateTransferPort());
                                server.addPeer(newPeer);

                                // Prepara la lista dei peer da inviare come risposta.
                                // Includi le informazioni del server che riceve la richiesta (cioè self) e gli altri peer già noti,
                                // escludendo il nodo che ha inviato la JOIN_REQUEST.
                                List<PeerInfo> responseList = new ArrayList<>();

                                // Ottieni l'host locale (puoi usare InetAddress.getLocalHost() oppure avere già salvato l'host nel Server).
                                String selfHost = InetAddress.getLocalHost().getHostAddress();
                                // Crea il PeerInfo per il server attuale (self).
                                PeerInfo selfPeer = new PeerInfo(server.getServerId(), selfHost, server.getReplicationPort(), server.getDiscoveryPort(), server.getStateTransferPort());
                                responseList.add(selfPeer);

                                // Aggiungi gli altri peer (escludi il nodo che ha inviato la richiesta, per evitare duplicati).
                                for (PeerInfo p : server.getPeerServers()) {
                                    if (!p.getServerId().equals(msg.getServerId())) {
                                        responseList.add(p);
                                    }
                                }

                                // Invia la risposta con la lista dei peer.
                                DiscoveryMessage response = new DiscoveryMessage(DiscoveryMessage.Type.JOIN_RESPONSE, responseList);
                                out.writeObject(response);
                                out.flush();
                                System.out.println("Processed JOIN_REQUEST from " + msg.getServerId());
                            }
                            if (msg.getType() == DiscoveryMessage.Type.NEW_PEER) {
                                PeerInfo newPeer = msg.getNewPeer();
                                // Aggiungi solo se non già presente
                                server.addPeer(newPeer);
                                server.getLocalClock().addServer(newPeer.getServerId());
                                System.out.println("Ricevuto NEW_PEER: " + newPeer);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Discovery message error: " + e.getMessage());
                    } finally {
                        try { socket.close(); } catch (IOException e) { }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Discovery listener error: " + e.getMessage());
        }
    }
}
