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

            CustomInetAddress successorPredecessor = this.node.getSuccessor() == null ?
                    null :
                    Utilities.addressRequest(this.node.getSuccessor(), "YOUR_PREDECESSOR");

            if(this.node.getSuccessor() != null && !this.node.getPeerAddress().equals(successorPredecessor)) {

                long successorID = Utilities.hashCode(this.node.getSuccessor().getHostAddress(), this.node.getSuccessor().getPort());
                long successorPredecessorID = Utilities.hashCode(Objects.requireNonNull(successorPredecessor).getHostAddress(), successorPredecessor.getPort());

                if(successorPredecessorID > this.node.getID() && successorPredecessorID < successorID)
                    this.node.setIthFinger(1, successorPredecessor);

                this.node.notifyNode(this.node.getSuccessor());
            }

        }
    }

    public void terminate() {
        this.online = false;
    }
}