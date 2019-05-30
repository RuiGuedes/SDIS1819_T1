package chord;

/**
 * Status class
 */
public class Status extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * Prints node information
     * @param node Associated Node
     */
    Status(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {
        while(online) {

            System.out.println("Node Information");
            System.out.println("ID -> " + this.node.getID());
            System.out.println("IP -> " + this.node.getAddress().getHostAddress());
            System.out.println("Port -> " + this.node.getAddress().getPort());
            System.out.println("\nFinger Table:");

            for(int i = 1; i <= Chord.M; i++) {
                if(this.node.getIthFinger(i) == null)
                    System.out.println(i + " -> " + this.node.getIthFinger(i));
                else
                    System.out.println(i + " -> " + Utilities.hashCode(this.node.getIthFinger(i).getHostAddress(), this.node.getIthFinger(i).getPort()));
            }

            if(this.node.getPredecessor() == null)
                System.out.println("9 -> " + this.node.getPredecessor());
            else
                System.out.println("9 -> " + Utilities.hashCode(this.node.getPredecessor().getHostAddress(), this.node.getPredecessor().getPort()));

            System.out.println("------------------------------------------------------------");

            try {
                Thread.sleep(5000);
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
