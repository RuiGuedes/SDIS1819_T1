package chord;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 *  PredecessorPolling class
 */
public class PredecessorPolling extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * PredecessorPolling class constructor
     * @param node Associated node
     */
    PredecessorPolling(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {

        while (online) {
            // Check whether predecessor is not null and online
            if (this.node.getPredecessor() != null) {
                String predecessorStatus = Utilities.sendRequest(this.node.getPredecessor(), "ONLINE");

                if (predecessorStatus == null || !predecessorStatus.equals("TRUE"))
                    this.node.clearPredecessor();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set online status to false
     */
    void terminate() {
        this.online = false;
    }

}

