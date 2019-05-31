package chord;

import java.util.HashMap;

class Node {

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
     * Class responsible to ensure predecessor validity
     */
    private PredecessorPolling predecessorPolling;

    /**
     * Chord status
     */
    private Status status;

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
        this.fixFingers = new FixFingers(this);
        this.stabilizer = new Stabilizer(this);
        this.nodeListener = new NodeListener(this);
        this.predecessorPolling = new PredecessorPolling(this);
        this.status = new Status(this);
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

        this.initializeAllThreads(true);

        return true;
    }

    /**
     * Initializes all threads
     */
    private void initializeAllThreads(boolean initStatus) {
        this.nodeListener.start();
        this.stabilizer.start();
        this.fixFingers.start();
        this.predecessorPolling.start();
        if(initStatus) this.status.start();
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
     * Notifies a certain node to update its successor
     * @param nodeToNotify Node to be notified
     */
    void notifyNode(CustomInetAddress nodeToNotify) {
        if(nodeToNotify != null && !nodeToNotify.equals(this.peerAddress)) {
            Utilities.sendRequest(nodeToNotify, "SET_PREDECESSOR:" + this.peerAddress.toString());
        }
    }

    /**
     * Upon a given notification, check if predecessor should be updated
     * @param newPredecessor New predecessor
     */
    void handleNodeNotification(CustomInetAddress newPredecessor) {
        if(this.predecessor == null)
            this.predecessor = newPredecessor;
        else {
            long predecessorID = Utilities.hashCode(this.predecessor.getHostAddress(), this.predecessor.getPort());
            long newPredecessorID = Utilities.hashCode(newPredecessor.getHostAddress(), newPredecessor.getPort());

            if(Utilities.belongsToInterval(newPredecessorID, predecessorID, this.getID()))
                this.predecessor = newPredecessor;
        }
    }

    /**
     * Updates finger on finger table. If it updates the first entry of the finger table (Successor)
     * it notifies it as it new predecessor is the current node
     * @param key Finger key
     * @param value Finger value
     */
    void setIthFinger(Integer key, CustomInetAddress value) {
        this.fingerTable.put(key, value);

        // If successor: notify the new successor
        if (key == 1 && value != null && !value.equals(this.peerAddress))
            this.notifyNode(value);
    }

    /**
     * Retrieves a certain finger identified by key
     * @param key Finger identifier
     */
    CustomInetAddress getIthFinger(Integer key) {
        return this.fingerTable.getOrDefault(key, null);
    }

    /**
     * Updates finger table successors based up on the its valid referenced nodes or its predecessor
     * if no entry in the finger table is valid
     */
    void updateCandidateSuccessors() {

        // Retrieve successor
        CustomInetAddress successor = this.getSuccessor();

        // Check if in the meanwhile successor got updated
        if(successor != null && !successor.equals(this.peerAddress))
            return;

        for (int index = 2; index <= Chord.M; index++) {
            // Retrieve finger table specific element
            CustomInetAddress fingerTableEntry = this.fingerTable.getOrDefault(index, null);

            if (fingerTableEntry != null && !fingerTableEntry.equals(this.peerAddress)) {
                // Updates previous finger table entry with the new successor candidate
                for (int j = index - 1; j >= 1; j--)
                    this.setIthFinger(j, fingerTableEntry);

                break;
            }
        }

        // If a valid finger table entry was not found consider the successor the same as the predecessor
        if ((this.getSuccessor() == null || this.getSuccessor().equals(this.peerAddress)) &&
                this.predecessor != null && !this.predecessor.equals(this.peerAddress)) {
            this.setIthFinger(1, this.predecessor);
        }
    }

    /**
     * Resets previously retrieved candidate successors
     */
    void resetCandidateSuccessors() {
        // If successor is null, its removal is not necessary
        if (this.getSuccessor() == null)
            return;

        // Delete all finger table entry that contains successor as its value
        this.clearSpecificFinger(this.getSuccessor());

        // Check successor and predecessor equality
        if (predecessor!= null && predecessor.equals(this.getSuccessor()))
            this.predecessor = null;

        // Fill successor with candidate values
        this.updateCandidateSuccessors();

        // If predecessor is a remote node, finds it predecessor to avoid entering a loop
        if ((this.getSuccessor() == null || this.getSuccessor().equals(this.peerAddress)) &&
                this.predecessor != null && !this.predecessor.equals(this.peerAddress)) {

            while (true) {
                CustomInetAddress newPredecessor = Utilities.addressRequest(this.predecessor, "YOUR_PREDECESSOR");

                if (newPredecessor == null || newPredecessor.equals(this.predecessor) ||
                        newPredecessor.equals(this.peerAddress) || newPredecessor.equals(this.getSuccessor()))
                    break;
                else
                    this.predecessor = newPredecessor;
            }

            // Set new node successor
            this.setIthFinger(1, this.predecessor);
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
    CustomInetAddress findSuccessor(long ID) {

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
    CustomInetAddress findPredecessor(long ID) {

        // Initialize node and successor variables
        CustomInetAddress node = this.peerAddress;
        CustomInetAddress nodeSuccessor = this.getSuccessor();
        CustomInetAddress lastValidNode = this.peerAddress;

        // If current peer does not have an already valid successor
        if(nodeSuccessor == null)
            return this.peerAddress;

        while(!(Utilities.belongsToInterval(ID, node.getNodeID(), nodeSuccessor.getNodeID()) || ID == nodeSuccessor.getNodeID())) {

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

                if(Utilities.belongsToInterval(node_id, this.peerID, ID)) {
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
     * Terminates all initialized threads
     */
    void terminateAllThreads() {
        if(this.nodeListener != null) this.nodeListener.terminate();
        if(this.stabilizer != null) this.stabilizer.terminate();
        if(this.fixFingers != null) this.fixFingers.terminate();
        if(this.predecessorPolling != null) this.predecessorPolling.terminate();
        if(this.status != null) this.status.terminate();
    }

    /**
     * Returns current node successor
     */
    CustomInetAddress getSuccessor() {
        return this.fingerTable.getOrDefault(1, null);
    }

    /**
     * Returns current node predecessor
     */
    CustomInetAddress getPredecessor() {
        return this.predecessor;
    }

    /**
     * Resets predecessor value to its default state: null
     */
    void clearPredecessor() {
        this.predecessor = null;
    }

    /**
     * Returns current node custom InetAddress
     */
    CustomInetAddress getAddress() {
        return this.peerAddress;
    }

    /**
     * Returns node ID
     */
    long getID() {
        return peerID;
    }
}
