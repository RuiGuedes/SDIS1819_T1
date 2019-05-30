package chord;

import storage.ChunkStorage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * NodeListener class
 */
public class NodeListener extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Datagram socket used for communication
     */
    private DatagramSocket socket = null;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online = true;

    /**
     * NodeListener class constructor
     * @param node Associated node
     */
    NodeListener(Node node) {
        this.node = node;
        CustomInetAddress address = this.node.getAddress();

        try {
            this.socket = new DatagramSocket(address.getPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(online) {
            byte[] buf = new byte[100];
            DatagramPacket receive = new DatagramPacket(buf, buf.length);

            try {
                this.socket.receive(receive);
                new Thread( new DecryptMessage(this.socket, receive, this.node)).start();
            } catch (IOException e) {
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

/**
 * DecryptMessage class
 */
class DecryptMessage extends Thread {

    /**
     * Received message
     */
    private String[] message;

    /**
     * Node associated InetAddress
     */
    private InetAddress address;

    /**
     * Node associated port
     */
    private Integer port;

    /**
     * Datagram socket used for communication
     */
    private DatagramSocket socket;

    /**
     * Associated node
     */
    private Node node;

    /**
     * DecryptMessage class constructor
     */
    DecryptMessage(DatagramSocket socket, DatagramPacket packet, Node node) {
        this.socket = socket;
        this.message = cleanString(packet.getData()).split(":");
        this.address = packet.getAddress();
        this.port = packet.getPort();
        this.node = node;
    }

    @Override
    public void run() {
        String response = null;

        switch(message[0]){
            case "FIND_SUCCESSOR":
                response = this.node.findSuccessor(Integer.parseInt(message[1])).toString();
                break;
            case "YOUR_SUCCESSOR":
                response = this.node.getSuccessor().toString();
                break;
            case "YOUR_PREDECESSOR":
                response = this.node.getPredecessor() == null ? null : this.node.getPredecessor().toString();
                break;
            case "SET_PREDECESSOR":
                this.node.handleNodeNotification(new CustomInetAddress(message[1], Integer.parseInt(message[2])));
                response = "SUCCESS";
                break;
            case "CLP_FINGER":
                response = this.node.closestPrecedingFinger(Integer.parseInt(message[1])).toString();
                break;
            case "ONLINE":
                response = "TRUE";
                break;
            case "DELETE_CHUNK":
                try {
                    ChunkStorage.delete(message[1]);
                    response = "SUCCESS";
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "FAIL";
                }
                break;
        }

        // Check if response if valid
        if (response == null) response = "EMPTY";

        byte[] buf = response.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleans byte array to contain only needed information
     * @param info Array received
     * @return Array content as a string
     */
    static String cleanString(byte[] info) {
        String message = null;
        char[] aux = new String(info, StandardCharsets.UTF_8).toCharArray();

        for (int i = 0; i < aux.length; i++){
            if(aux[i] == 0) {
                message = String.valueOf(Arrays.copyOf(aux, i));
                break;
            }
        }
        return  message;
    }
}