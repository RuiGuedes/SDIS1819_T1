package chord;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Stabilizer class
 */
public class Stabilizer extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * Stabilizer class constructor
     * @param node Associated node
     */
    Stabilizer(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {
        while(online) {

            // Determines whether node successor is null or is its own node
            if(this.node.getSuccessor() == null || this.node.getSuccessor().equals(this.node.getAddress()))
                this.node.updateCandidateSuccessors();

            if(this.node.getSuccessor() != null && !this.node.getSuccessor().equals(this.node.getAddress())) {

                CustomInetAddress successorPredecessor = Utilities.addressRequest(this.node.getSuccessor(), "YOUR_PREDECESSOR");

                if(successorPredecessor != null) {
                    long successorID = Utilities.hashCode(this.node.getSuccessor().getHostAddress(), this.node.getSuccessor().getPort());
                    long successorPredecessorID = Utilities.hashCode(Objects.requireNonNull(successorPredecessor).getHostAddress(), successorPredecessor.getPort());

                    if(Utilities.belongsToInterval(successorPredecessorID, this.node.getID(), successorID))
                        this.node.setIthFinger(1, successorPredecessor);

                    this.node.notifyNode(this.node.getSuccessor());
                }
                else
                    this.node.resetCandidateSuccessors();
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