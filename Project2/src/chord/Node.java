package chord;

import java.net.InetAddress;

public class Node {

    /**
     * Peer InetAddress
     */
    private InetAddress peerAddress;

    /**
     * Peer associated Port
     */
    private Integer peerPort;

    /**
     * Peer network ID
     */
    private Integer peerdID;

    /**
     * Address of the node used to introduce current peer to the network
     */
    private InetAddress contactAddress;

    /**
     * Port of the node used to introduce current peer to the network
     */
    private Integer contactPort;


    Node(InetAddress address, Integer port) {
        this.peerAddress = address;
        this.peerPort = port;
    }

    boolean joinNetwork(InetAddress address, Integer port) {
        this.contactAddress = address;
        this.contactPort = port;


        return true;
    }

}
