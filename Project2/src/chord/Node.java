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
     *
     */
    private NodeListener nodeListener;

    /**
     *
     */
    private Stabilizer stabilizer;

    /**
     *
     */
    private FixFingers fixFingers;
    // private AskPredecessor

    /**
     *
     * @param address
     */
    Node(CustomInetAddress address) {
        // Variables Initialization
        this.peerAddress = address;
        this.predecessor = null;
        this.peerdID = Utilities.hashCode(this.peerAddress.getHostAddress(), this.peerAddress.getPort());
        this.initFingerTable();

        // Initialize all node associated threads
        this.fixFingers = new FixFingers();
        this.stabilizer = new Stabilizer();
        this.nodeListener = new NodeListener();
    }

    boolean joinNetwork(CustomInetAddress address) {
        this.contactAddress = address;

        return true;
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

}
