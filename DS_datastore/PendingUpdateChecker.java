package DS_datastore;

public class PendingUpdateChecker implements Runnable {
    private Server server;

    public PendingUpdateChecker(Server server) {
        this.server = server;
    }

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
