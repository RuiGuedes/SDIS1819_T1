package chord;

import java.util.Objects;

public class Stabilizer extends Thread {

    private Node node;

    private boolean online;

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

                    if(successorPredecessorID > this.node.getID() && successorPredecessorID < successorID)
                        this.node.setIthFinger(1, successorPredecessor);

                    this.node.notifyNode(this.node.getSuccessor());
                }
                else
                    this.node.resetCandidateSuccessors();
            }

        }
    }

    public void terminate() {
        this.online = false;
    }
}