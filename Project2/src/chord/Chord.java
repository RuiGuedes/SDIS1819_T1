package chord;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
    public static final Integer M = 8;

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

        // Variables declaration
        int peerPort, contactPort;
        InetAddress peerAddress, contactAddress;

        // Initialize variables
        peerPort = contactPort = Integer.parseInt(args[0]);
        peerAddress = contactAddress = Chord.createInetAddress(this.getHostAddress());

        // Initializes contact peer address
        if(args.length > 1) {
            String[] contactPeerInfo = args[1].split(":");

            contactPort = Integer.parseInt(contactPeerInfo[1]);
            contactAddress = Chord.createInetAddress(contactPeerInfo[0]);
        }

        // Checks whether any of the created is not valid
        if(peerAddress == null || contactAddress == null) {
            System.out.println("Contact address not found !");
            return false;
        }

        // Create local node
        Chord.node = new Node(peerAddress, peerPort);

        // Join network
        boolean joinStatus = Chord.node.joinNetwork(contactAddress, contactPort);

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

    /**
     * Creates InetAddress given an address
     * @param address Address to be used
     * @return InetAddress
     */
    private static InetAddress createInetAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if(inetAddress == null)
                throw new UnknownHostException();
            else
                return inetAddress;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves local host IP address
     * @return IP address
     */
    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

}