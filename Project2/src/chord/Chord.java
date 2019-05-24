package chord;

/**
 * Chord class
 */
public class Chord {

    /**
     * Value used to replicate information on both successors and predecessors
     */
    public static final Integer R = 1;

    /**
     * Value used to truncate ID's
     */
    static final Integer M = 8;

    /**
     * Created node
     */
    private static Node node;

    /**
     * Default constructor
     */
    public Chord() {}

    /**
     * Initializes chord by initialize all needed variable according information passed as parameter
     * @param args Arguments used to initialize node
     * @return True on success, false otherwise
     */
    public boolean initialize(String[] args) {

        // Declare and initialize variables
        CustomInetAddress peerAddress = new CustomInetAddress(Utilities.getHostAddress(), Integer.parseInt(args[0]));
        CustomInetAddress contactAddress = new CustomInetAddress(Utilities.getHostAddress(), Integer.parseInt(args[0]));

        // Initializes contact peer address
        if(args.length > 2) {
            String[] contactPeerInfo = args[1].split(":");
            contactAddress = new CustomInetAddress(contactPeerInfo[0], Integer.parseInt(contactPeerInfo[1]));
        }

        // Checks whether any of the created is not valid
        if(peerAddress.getAddress() == null || contactAddress.getAddress() == null) {
            System.out.println("One the following address's could not be found: local_address or contact_address !");
            return false;
        }

        // Create local node
        Chord.node = new Node(peerAddress);

        // Join network
        boolean joinStatus = Chord.node.joinNetwork(contactAddress);

        if(!joinStatus)
            System.out.println("Node could not join successfully the network !");
        else
            System.out.println("Node has joined successfully the network !");

        return joinStatus;
    }

    /**
     * Retrieves peer associated node
     * @return Node
     */
    public static Node getNode() {
        return node;
    }

}