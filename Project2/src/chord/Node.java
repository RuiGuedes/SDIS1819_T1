package chord;

import java.net.InetAddress;

public class Node {

    public InetAddress peerAddress;

    public Integer peerPort;

    public InetAddress contactAddress;

    public Integer contactPort;


    public Node(InetAddress address, Integer port) {
        this.peerAddress = address;
        this.peerPort = port;
    }

    public boolean joinNetwork(InetAddress address, Integer port) {
        this.contactAddress = address;
        this.contactPort = port;


        return true;
    }

}
