package DS_datastore;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private String serverId;
    private int clientPort;          // For client connections.
    private int replicationPort;     // For server-to-server updates.
    private int discoveryPort;       // For discovery messages.
    private int stateTransferPort;   // For state recovery.
    private Set<String> allServerIds;
    private KeyValueStore keyValueStore;
    private VectorClock localClock;
    private List<PeerInfo> peerServers; // Information about other servers.

    // New fields for peer discovery
    private String seedHost;         // IP address of an active server (if any)
    private int seedDiscoveryPort;   // Its discovery port
    private PeerInfo seedPeer;

    public static String getCorrectIP() throws SocketException {
        return java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces()).stream()
                .filter(i -> {
                    try {
                        return i.isUp() && !i.isLoopback() && !i.isVirtual();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .flatMap(i -> java.util.Collections.list(i.getInetAddresses()).stream())
                .filter(addr -> addr instanceof java.net.Inet4Address && addr.getHostAddress().startsWith("192.168.1."))
                .map(java.net.InetAddress::getHostAddress)
                .findFirst()
                .orElse("IP not founded");
    }

    // A buffer for updates that cannot yet be applied.
    private final List<UpdateMessage> pendingUpdates = new ArrayList<>();

    // Pending replications for omission failures.
    private final Map<PeerInfo, List<ReplicableMessage>> pendingReplications = new ConcurrentHashMap<>();

    // Modified constructor: now receives seed parameters.
    public Server(String serverId, int clientPort, int replicationPort, int discoveryPort, int stateTransferPort,
                  Set<String> allServerIds, List<PeerInfo> peerServers,
                  String seedHost, int seedDiscoveryPort) {
        this.serverId = serverId;
        this.clientPort = clientPort;
        this.replicationPort = replicationPort;
        this.discoveryPort = discoveryPort;
        this.stateTransferPort = stateTransferPort;
        this.allServerIds = allServerIds;
        this.peerServers = new ArrayList<>(peerServers);
        this.keyValueStore = new KeyValueStore();
        this.localClock = new VectorClock(allServerIds);
        this.seedHost = seedHost;
        this.seedDiscoveryPort = seedDiscoveryPort;
    }

    public void start() {
        // Start client listener thread.
        new Thread(new ClientListener(clientPort, this)).start();

        // Start replication listener thread.
        new Thread(new ReplicationListener(this)).start();

        // Start discovery listener thread.
        new Thread(new DiscoveryListener(this)).start();

        // Start state transfer listener thread.
        new Thread(new StateTransferListener(this)).start();

        // Start a thread to check pending updates periodically.
        new Thread(new PendingUpdateChecker(this)).start();

        // Start replication retry thread.
        new Thread(new ReplicationRetryThread(this)).start();

        // If seed is provided, join the network.
        if (seedHost != null && !seedHost.isEmpty()) {
            joinNetwork();
        }

        // Optionally recover state (if this server is restarting).
        recoverState();

        System.out.println("Server " + serverId + " started.");
    }


    public void broadcastMyPresence() {
        // Crea il proprio PeerInfo: assicurati di avere un getter per replicationPort e il serverId
        String selfHost;
        try {
            selfHost = getCorrectIP();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        PeerInfo selfPeer = new PeerInfo(serverId, selfHost, replicationPort, discoveryPort, stateTransferPort);

        ReplicableMessage newPeerMsg = new DiscoveryMessage(DiscoveryMessage.Type.NEW_PEER, selfPeer);

        // Invia un messaggio NEW_PEER a ciascun peer conosciuto, ad eccezione di se stesso.
        for (PeerInfo peer : getPeerServers()) {
            if (!peer.getServerId().equals(this.serverId) && (seedPeer == null || !peer.equals(seedPeer))) {
                // usa quella; qui supponiamo che ogni peer ascolti sul proprio discoveryPort.
                int peerDiscoveryPort = peer.getDiscoveryPort();
                new Thread(() -> {
                    try (Socket socket = new Socket(peer.getHost(), peerDiscoveryPort);
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        DiscoveryMessage msg = new DiscoveryMessage(DiscoveryMessage.Type.NEW_PEER, selfPeer);
                        out.writeObject(msg);
                        out.flush();
                        System.out.println("Inviato NEW_PEER a " + peer);
                    } catch (Exception e) {
                        System.err.println("Errore durante il broadcast a " + peer + ": " + e.getMessage());
                        pendingReplications.computeIfAbsent(peer, k -> new ArrayList<>()).add(newPeerMsg);
                    }
                }).start();
            }
        }
    }



    // This method contacts the seed server and obtains the list of known peers.
    private void joinNetwork() {
        try (Socket socket = new Socket(seedHost, seedDiscoveryPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Create and send a JOIN_REQUEST using DiscoveryMessage.
            DiscoveryMessage joinRequest = new DiscoveryMessage(DiscoveryMessage.Type.JOIN_REQUEST,
                    serverId, InetAddress.getLocalHost().getHostAddress(), replicationPort, discoveryPort, stateTransferPort);
            out.writeObject(joinRequest);
            out.flush();

            // Wait for the JOIN_RESPONSE.
            Object responseObj = in.readObject();
            if (responseObj instanceof DiscoveryMessage) {
                DiscoveryMessage response = (DiscoveryMessage) responseObj;
                if (response.getType() == DiscoveryMessage.Type.JOIN_RESPONSE) {
                    List<PeerInfo> discoveredPeers = response.getPeerList();
                    for (PeerInfo peer : discoveredPeers) {

                        if(peer.getHost().equals(this.seedHost) && (peer.getDiscoveryPort()) == this.seedDiscoveryPort) {
                            this.seedPeer = peer;
                        }

                        if (!peer.getServerId().equals(this.serverId)) {
                            addPeer(peer);
                            localClock.addServer(peer.getServerId());
                        }
                    }
                    System.out.println("Joined network via seed. Discovered peers: " + discoveredPeers);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to join network via seed: " + e.getMessage());
        }

        broadcastMyPresence();
    }

    public synchronized void addPeer(PeerInfo peer) {
        boolean exists = peerServers.stream()
                .anyMatch(p -> p.getHost().equals(peer.getHost()) && p.getReplicationPort() == peer.getReplicationPort());
        if (!exists) {
            peerServers.add(peer);
            System.out.println("Added new peer: " + peer.getHost() + ":" + peer.getReplicationPort());
        }
    }


    // Recover state from one of the known peers.
    private void recoverState() {
        if (peerServers.isEmpty()) {
            System.out.println("No peers available for state recovery.");
            return;
        }
        // Pick the first peer.
        PeerInfo peer = peerServers.get(0);
        try {
            int peerStatePort = peer.getStateTransferPort();
            try (Socket socket = new Socket(peer.getHost(), peerStatePort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send state request.
                out.writeObject(new StateRequestMessage());
                out.flush();

                Object responseObj = in.readObject();
                if (responseObj instanceof StateResponseMessage) {
                    StateResponseMessage response = (StateResponseMessage) responseObj;
                    // Recover the key-value store and vector clock.
                    Map<String, ValueEntry> snapshot = response.getStoreSnapshot();
                    for (Map.Entry<String, ValueEntry> entry : snapshot.entrySet()) {
                        keyValueStore.write(entry.getKey(), entry.getValue().getValue(), entry.getValue().getVectorClock());
                    }
                    localClock.merge(response.getVectorClock());
                    System.out.println("State recovered from peer " + peer.getHost());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to recover state from peer " + peer.getHost() + ": " + e.getMessage());
        }
    }

    // Methods to handle local writes from clients.
    public synchronized void handleLocalWrite(String key, String value) {
        // Increment the local vector clock.
        localClock.increment(serverId);
        // Write locally.
        keyValueStore.write(key, value, localClock);
        System.out.println("Local write applied on key: " + key + " value: " + value + " VC: " + localClock);
        // Create an UpdateMessage to replicate.
        UpdateMessage update = new UpdateMessage(key, value, serverId, localClock);
        // Replicate update.
        replicateUpdate(update);
    }

    public void replicateUpdate(UpdateMessage update) {
        for (PeerInfo peer : peerServers) {
            new Thread(() -> {
                try (Socket socket = new Socket(peer.getHost(), peer.getReplicationPort());
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    out.writeObject(update);
                    out.flush();
                } catch (Exception e) {
                    System.err.println("Replication to " + peer.getHost() + ":" + peer.getReplicationPort() + " failed: " + e.getMessage());
                    pendingReplications.computeIfAbsent(peer, k -> new ArrayList<>()).add(update);
                }
            }).start();
        }
    }

    public synchronized void handleRemoteUpdate(UpdateMessage update) {
        if (localClock.canApply(update.getOriginServerId(), update.getVectorClock())) {
            keyValueStore.write(update.getKey(), update.getValue(), update.getVectorClock());
            localClock.merge(update.getVectorClock());
            System.out.println(localClock.toString());
            System.out.println("Remote update applied for key: " + update.getKey() + " value: " + update.getValue() + " Last Recived Message' VC: " + update.getVectorClock());
            checkPendingUpdates();
        } else {
            pendingUpdates.add(update);
            System.out.println("Remote update buffered for key: " + update.getKey());
        }
    }

    public synchronized void checkPendingUpdates() {
        pendingUpdates.removeIf(pending -> {
            if (localClock.canApply(pending.getOriginServerId(), pending.getVectorClock())) {
                keyValueStore.write(pending.getKey(), pending.getValue(), pending.getVectorClock());
                localClock.merge(pending.getVectorClock());
                System.out.println("Pending update applied for key: " + pending.getKey());
                return true;
            }
            return false;
        });
    }

    public synchronized String handleLocalRead(String key) {
        return keyValueStore.read(key);
    }

    public String getServerId() {
        return serverId;
    }

    public VectorClock getLocalClock() {
        return localClock;
    }

    public synchronized List<PeerInfo> getPeerServers() {
        return new ArrayList<>(peerServers);
    }

    public Map<String, ValueEntry> getKeyValueStoreSnapshot() {
        return keyValueStore.getStoreSnapshot();
    }

    public Map<PeerInfo, List<ReplicableMessage>> getPendingReplications() {
        return pendingReplications;
    }

    public int getReplicationPort() {
        return replicationPort;
    }

    public int getDiscoveryPort() {
        return discoveryPort;
    }

    public int getStateTransferPort() {
        return stateTransferPort;
    }




}
