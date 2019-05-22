package chord;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Node {

    /**
     * Peer CustomInetAddress
     */
    private CustomInetAddress peerAddress;

    /**
     * Peer network ID
     */
    private long peerdID;

    /**
     * Node finger table
     */
    private HashMap<Integer, CustomInetAddress> fingerTable;

    /**
     * Node predecessor
     */
    private CustomInetAddress predecessor;

    /**
     * Address of the node used to introduce current peer to the network
     */
    private CustomInetAddress contactAddress;

    /**
     * Listener responsible for receiving messages from other nodes
     */
    private NodeListener nodeListener;

    /**
     * Class responsible for the stabilization protocol
     */
    private Stabilizer stabilizer;

    /**
     * Class responsible for fixing the finger table at long term
     */
    private FixFingers fixFingers;

    /**
     * Node constructor. Initializes all node class needed variables
     * @param address
     */
    Node(CustomInetAddress address) {
        // Variables Initialization
        this.peerAddress = address;
        this.peerdID = Utilities.hashCode(this.peerAddress.getHostAddress(), this.peerAddress.getPort());
        this.initFingerTable();

        // Initialize all node associated threads
        this.fixFingers = new FixFingers();
        this.stabilizer = new Stabilizer();
        this.nodeListener = new NodeListener(this);
    }

    /**
     * Join node to the network through a node that already belongs to the network
     * @param address Contact peer
     * @return True if it has success, false otherwise
     */
    boolean joinNetwork(CustomInetAddress address) {
        this.predecessor = null;
        this.contactAddress = address;

        if(this.contactAddress != null && !this.contactAddress.equals(this.peerAddress)) {
            CustomInetAddress successor = Utilities.sendRequest(this.contactAddress, "FIND_SUCCESSOR:" + this.peerdID);

            if(successor == null) {
                System.out.println("Join was not possible due to system being unable to find contact node");
                return false;
            }

            this.setIthFinger(1, successor);
        }

        this.initializeAllThreads();

        return true;
    }

    /**
     * Initializes all threads
     */
    private void initializeAllThreads() {
        this.nodeListener.start();
        this.stabilizer.start();
        this.fixFingers.start();
    }

    /**
     * Initializes node finger table with null values
     */
    private void initFingerTable() {
        // Initialize finger table
        this.fingerTable = new HashMap<>();

        for (int index = 1; index <= Chord.M; index++) {
            this.fingerTable.put(index, null);
        }
    }

    /**
     * Get Custom InetAddress
     * @return Custom InetAddres
     */
    public CustomInetAddress getPeerAddress() {
        return peerAddress;
    }

    /**
     * Get value of finger table
     * @param key key of the value
     * @return Custom InetAddress
     */
    public CustomInetAddress getFingerTable(Integer key) {
        return fingerTable.get(key);
    }

    /**
     * Updates finger on finger table. If it updates the first entry of the finger table (Successor)
     * it notifies it as it new predecessor is the current node
     * @param key Finger key
     * @param value Finger value
     */
    private void setIthFinger(Integer key, CustomInetAddress value) {
        this.fingerTable.put(key, value);

        // If successor: updates successor if the updated one is successor, notify the new successor
        if (key == 1 && value != null && !value.equals(this.peerAddress)) {
            // notify(value);
        }
    }
}
