package chord;

import java.util.HashMap;

public class Node {

    /**
     * Peer CustomInetAddress
     */
    private CustomInetAddress peerAddress;

    /**
     * Peer network ID
     */
    private long peerID;

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
     * @param address Node address
     */
    Node(CustomInetAddress address) {
        // Variables Initialization
        this.predecessor = null;
        this.peerAddress = address;
        this.peerID = Utilities.hashCode(this.peerAddress.getHostAddress(), this.peerAddress.getPort());

        // Init finger table
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
        this.contactAddress = address;

        if(this.contactAddress != null && !this.contactAddress.equals(this.peerAddress)) {
            CustomInetAddress successor = Utilities.addressRequest(this.contactAddress, "FIND_SUCCESSOR:" + this.peerID);

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

    /**
     * Removes an element from the finger table
     * @param value Element to be removed
     */
    private void clearSpecificFinger(CustomInetAddress value) {
        for(int index = Chord.M; index >= 1; index--) {
            // Retrieve element from the finger table
            CustomInetAddress fingerTableEntry = this.fingerTable.get(index);

            if(fingerTableEntry != null && fingerTableEntry.equals(value))
                this.fingerTable.put(index, null);
        }
    }

    /**
     * Finds the successor of node with the ID passed by parameter
     * @param ID Node ID
     * @return Successor of the node
     */
    public CustomInetAddress findSuccessor(long ID) {

        // Assuming the node predecessor is the current node
        CustomInetAddress successor = this.getSuccessor();

        CustomInetAddress predecessor = this.findPredecessor(ID);

        // Checks whether node predecessor is equals to the current node
        if(!predecessor.equals(this.peerAddress))
            successor = Utilities.addressRequest(predecessor, "YOUR_SUCCESSOR");

        // If successor is null, return the current node as the successor
        return successor == null ? this.peerAddress : successor;
    }

    /**
     * Finds the predecessor of a node with the ID passed by parameter
     * @param ID Node ID
     * @return Predecessor of the node
     */
    private CustomInetAddress findPredecessor(long ID) {

        // Initialize node and successor variables
        CustomInetAddress node = this.peerAddress;
        CustomInetAddress nodeSuccessor = this.getSuccessor();
        CustomInetAddress lastValidNode = this.peerAddress;

        while(ID <= node.getNodeID() || ID > nodeSuccessor.getNodeID()) {

            // Temporary node
            CustomInetAddress lastNode = node;

            if(node.equals(this.peerAddress)) {
                CustomInetAddress node_clpf = this.closestPrecedingFinger(ID);

                if(!node_clpf.equals(this.peerAddress)) {
                    // Update variables
                    nodeSuccessor = Utilities.addressRequest(node_clpf, "YOUR_SUCCESSOR");

                    // If nodeSuccessor is valid, update node
                    if (nodeSuccessor != null)
                        node = node_clpf;
                    else
                        nodeSuccessor = this.getSuccessor();
                }
            }
            else {
                // Retrieve remote node closest preceding finger
                CustomInetAddress node_clpf = Utilities.addressRequest(node, "CLP_FINGER:" + ID);

                if(node_clpf == null) {
                    // Update variables
                    node = lastValidNode;
                    nodeSuccessor = Utilities.addressRequest(node, "YOUR_SUCCESSOR");

                    // Check if its not possible to retrieve predecessor
                    if(nodeSuccessor == null)
                        return this.peerAddress;

                    continue;
                }
                else if(node_clpf.equals(node))
                    return node;
                else {
                    // Update variables
                    lastValidNode = node;
                    nodeSuccessor = Utilities.addressRequest(node_clpf, "YOUR_SUCCESSOR");

                    // If nodeSuccessor is valid, update node
                    if (nodeSuccessor != null)
                        node = node_clpf;
                    else
                        nodeSuccessor = Utilities.addressRequest(node, "YOUR_SUCCESSOR");
                }
            }

            // Checks if last node is the same as the new one
            if(lastNode.equals(node))
                break;
        }

        return node;
    }


    /**
     * Return the closest finger preceding ID
     * @param ID Node ID
     */
    CustomInetAddress closestPrecedingFinger(long ID) {
        for(int index = Chord.M; index >= 1; index--) {
            // Retrieves the value from the finger table
            CustomInetAddress node = this.fingerTable.getOrDefault(index, null);

            // If not null check if ID is in the respective interval
            if(node != null) {
                long node_id = node.getNodeID();

                if(node_id > this.peerID && node_id < ID) {
                    // Determine if node is online
                    if(Utilities.sendRequest(node, "ONLINE").equals("TRUE"))
                        return node;
                    else
                        this.clearSpecificFinger(node);
                }
            }
        }

        // If any value on the finger table does not meet the needed constraints return current node
        return this.peerAddress;
    }

    /**
     * Returns current node successor
     */
    CustomInetAddress getSuccessor() {
        return this.fingerTable.getOrDefault(1, null);
    }

    CustomInetAddress getPeerAddress() {
        return this.peerAddress;
    }
}
