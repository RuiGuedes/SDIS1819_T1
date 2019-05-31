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
        String[] peerInfo = args[0].split(":");
        CustomInetAddress peerAddress = new CustomInetAddress(peerInfo[0], Integer.parseInt(peerInfo[1]));
        CustomInetAddress contactAddress = new CustomInetAddress(peerInfo[0], Integer.parseInt(peerInfo[1]));

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

        // Start Key Transfer thread
        new Thread(new TransferKeys(Chord.node));

        if(!joinStatus)
            System.out.println("Node could not join successfully the network!");
        else
            System.out.println("Node has joined successfully the network!");

        return joinStatus;
    }

    static Node getNode() {
        return node;
    }
}