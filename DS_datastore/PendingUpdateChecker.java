package DS_datastore;

/**
 * Periodically checks for pending updates that need to be processed by the server.
 * <p>
 * This class runs in its own thread and invokes {@link Server#checkPendingUpdates()}
 * every second. It is useful for background maintenance tasks or eventual consistency handling
 * in distributed systems.
 */
public class PendingUpdateChecker implements Runnable {
    /** Reference to the server instance to invoke update checks on. */
    private Server server;

    /**
     * Constructs a {@code PendingUpdateChecker} with the given server reference.
     *
     * @param server the server whose pending updates should be checked periodically
     */
    public PendingUpdateChecker(Server server) {
        this.server = server;
    }

    /**
     * Continuously runs in a loop, sleeping for one second between each iteration.
     * On each iteration, it calls {@link Server#checkPendingUpdates()} to check
     * and process any outstanding updates.
     * <p>
     * If the thread is interrupted, it will gracefully exit the loop.
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000); // check every second
                server.checkPendingUpdates();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
