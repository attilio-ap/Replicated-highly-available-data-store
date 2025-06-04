package DS_datastore;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a server node in a distributed key-value store system.
 * <p>
 * Each server handles:
 * <ul>
 *     <li>Client requests via TCP</li>
 *     <li>Replication of updates to peer servers</li>
 *     <li>Peer discovery and joining the network via a seed node</li>
 *     <li>State recovery from peers during startup</li>
 *     <li>Managing and applying updates with vector clocks</li>
 * </ul>
 * This class is the central orchestrator of all listener threads, replication mechanisms,
 * state recovery, and update consistency.
 */
public class Server {
    /** Unique identifier of this server. */
    private String serverId;

    /** Port for handling client connections. */
    private int clientPort;

    /** Port for replication traffic from other servers. */
    private int replicationPort;

    /** Port for peer discovery communication. */
    private int discoveryPort;

    /** Port for state transfer operations. */
    private int stateTransferPort;

    /** Set of all server IDs in the system. */
    private Set<String> allServerIds;

    /** In-memory key-value store. */
    private KeyValueStore keyValueStore;

    /** Local vector clock for causal consistency. */
    private VectorClock localClock;

    /** List of peer server information. */
    private List<PeerInfo> peerServers;

    /** Hostname or IP of a known server to join an existing network (optional). */
    private String seedHost;

    /** Discovery port of the seed server. */
    private int seedDiscoveryPort;

    /** Reference to the peer info of the seed node (set during joining). */
    private PeerInfo seedPeer;

    /** List of updates that are waiting for causal readiness. */
    private final List<UpdateMessage> pendingUpdates = new ArrayList<>();

    /** Pending replication messages for retry in case of failure. */
    private final Map<PeerInfo, List<ReplicableMessage>> pendingReplications = new ConcurrentHashMap<>();

    /**
     * Returns the machine's IPv4 address starting with "192.168.1.".
     *
     * @return the local IP address if found, otherwise a fallback string
     * @throws SocketException if there is an issue accessing network interfaces
     */
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

    /**
     * Constructs a new server with all required configuration parameters.
     *
     * @param serverId           unique server identifier
     * @param clientPort         port for client communication
     * @param replicationPort    port for replication messages
     * @param discoveryPort      port for peer discovery
     * @param stateTransferPort  port for state transfer
     * @param allServerIds       all known server IDs
     * @param peerServers        initial peer list
     * @param seedHost           address of seed server (nullable)
     * @param seedDiscoveryPort  discovery port of the seed server
     */
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

    /**
     * Starts all background services for the server:
     * <ul>
     *     <li>Client listener</li>
     *     <li>Replication listener</li>
     *     <li>Discovery listener</li>
     *     <li>State transfer listener</li>
     *     <li>Pending update checker</li>
     *     <li>Replication retry thread</li>
     * </ul>
     * If a seed server is provided, it attempts to join the network.
     */
    public void start() {
        // Start client listener thread.
        new Thread(new ClientListener(clientPort, this)).start();

        // Start replication listener thread.
        new Thread(new ReplicationListener(this)).start();

        // Start discovery listener thread.
        new Thread(new DiscoveryListener(this)).start();

        // Start state transfer listener thread.
        new Thread(new StateTransferListener(this)).start();

        // Start replication retry thread.
        new Thread(new ReplicationRetryThread(this)).start();

        // If seed is provided, join the network.
        if (seedHost != null && !seedHost.isEmpty()) {
            joinNetwork();
        }
        System.out.println("Server " + serverId + " started.");
    }


    /**
     * Connects to the seed server and obtains the list of known peers via a JOIN_REQUEST.
     * Updates local peer list and vector clock based on the response.
     * Then broadcasts presence to the discovered peers.
     */
    private synchronized void joinNetwork() {

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


                        if( (peer.getHost().equals(this.seedHost)) && (peer.getDiscoveryPort() == this.seedDiscoveryPort)) {
                            this.seedPeer = peer;
                        }

                        if (!peer.getServerId().equals(this.serverId)) {
                            addPeer(peer);
                            localClock.addServer(peer.getServerId());
                        }
                    }

                    System.out.println("Joined network via seed. Discovered peers: " + discoveredPeers);
                    broadcastMyPresence();
                    recoverState();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to join network via seed: " + e.getMessage());
        }
    }


    /**
     * Sends a NEW_PEER message to all known peers to announce this server's presence.
     * If delivery fails, the message is queued in {@code pendingReplications}.
     */
    public void broadcastMyPresence() {
        // Create its own PeerInfo
        String selfHost;
        try {
            selfHost = getCorrectIP();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        PeerInfo selfPeer = new PeerInfo(serverId, selfHost, replicationPort, discoveryPort, stateTransferPort);

        ReplicableMessage newPeerMsg = new DiscoveryMessage(DiscoveryMessage.Type.NEW_PEER, selfPeer);

        // Send NEW_PEER to all known peers, except itsself.
        for (PeerInfo peer : getPeerServers()) {
            if (!peer.getServerId().equals(this.serverId) && (seedPeer == null || !peer.equals(seedPeer))) {
                // each peer listen to its discoveryPort.
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



    /**
     * Adds a new peer to the local peer list if not already present.
     *
     * @param peer the new peer to add
     */
    public synchronized void addPeer(PeerInfo peer) {
        boolean exists = peerServers.stream()
                .anyMatch(p -> p.getHost().equals(peer.getHost()) && p.getReplicationPort() == peer.getReplicationPort());
        if (!exists) {
            peerServers.add(peer);
            System.out.println("Added new peer: " + peer.getHost() + ":" + peer.getReplicationPort());
        }
    }



    /**
     * Attempts to recover the current state (key-value store and vector clock)
     * from the first available peer in the list using a state transfer protocol.
     */
    private void recoverState() {
        if (peerServers.isEmpty()) {
            System.out.println("No peers available for state recovery.");
            return;
        }
        // Pick the first peer.
        PeerInfo peer = seedPeer;
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



    /**
     * Handles a local write operation requested by a client.
     * Increments the local vector clock, updates the store, and replicates the update.
     *
     * @param key   the key to write
     * @param value the value to associate
     */
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

    /**
     * Sends the update message to all known peers.
     * If a peer is unreachable, the message is queued for retry.
     *
     * @param update the update to replicate
     */
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

    /**
     * Applies an update received from another server if the vector clock allows it.
     * Otherwise, queues it in {@code pendingUpdates} until it can be applied.
     *
     * @param update the remote update to apply
     */
    public synchronized void handleRemoteUpdate(UpdateMessage update) {

    /* 1) Se l’update è obsoleto (tutti i suoi timestamp ≤ al mio),
          lo scarto immediatamente.                                   */
        if (localClock.dominates(update.getVectorClock())) {
            System.out.println("Ignored obsolete update for key "
                    + update.getKey() + " VC=" + update.getVectorClock());
            return;   // niente altro da fare
        }

    /* 2) Caso normale: verifico se posso applicarlo ora,
          altrimenti lo metto in pending.                              */
        if (localClock.canApply(update.getOriginServerId(),
                update.getVectorClock())) {

            keyValueStore.write(update.getKey(),
                    update.getValue(),
                    update.getVectorClock());
            localClock.merge(update.getVectorClock());

            System.out.println(localClock);
            System.out.println("Remote update applied for key: "
                    + update.getKey() + " value: " + update.getValue()
                    + " VC: " + update.getVectorClock());

            checkPendingUpdates();

        } else {
            pendingUpdates.add(update);
            System.out.println("Remote update buffered for key: "
                    + update.getKey());
        }
    }


    /**
     * Checks the pending updates list and applies any update whose vector clock now allows it.
     */
    /**
     * Tenta di applicare tutti gli update pendenti finché
     * non ci sono più progressi in un intero giro.
     */
    public synchronized void checkPendingUpdates() {
        boolean progress;
        do {
            progress = false;
            Iterator<UpdateMessage> it = pendingUpdates.iterator();
            while (it.hasNext()) {
                UpdateMessage pending = it.next();
                if (localClock.canApply(pending.getOriginServerId(),
                        pending.getVectorClock())) {

                    keyValueStore.write(pending.getKey(),
                            pending.getValue(),
                            pending.getVectorClock());
                    localClock.merge(pending.getVectorClock());
                    it.remove();
                    System.out.println("Pending update applied for key: "
                            + pending.getKey());
                    progress = true;
                }
            }
        } while (progress);
    }

    /**
     * Handles a local read request from a client.
     *
     * @param key the key to read
     * @return the associated value, or {@code null} if not found
     */
    public synchronized String handleLocalRead(String key) {
        return keyValueStore.read(key);
    }

    // === Getters ===

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
