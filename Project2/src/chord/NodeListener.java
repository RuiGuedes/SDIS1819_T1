package chord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NodeListener extends Thread {

    private Node node;
    private DatagramSocket socket = null;
    private boolean online = true;

    NodeListener(Node n) {
        this.node = n;
        CustomInetAddress address = n.getPeerAddress();

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
     * Terminate the thread
     */
    public void terminate() {
        this.online = false;
    }
}

class DecryptMessage extends Thread {

    private String[] message;
    private InetAddress address;
    private Integer port;
    private DatagramSocket socket;
    private Node node;

    DecryptMessage(DatagramSocket socket, DatagramPacket packet, Node n) {
        this.socket = socket;
        this.message = cleanString(packet.getData()).split(":");
        this.address = packet.getAddress();
        this.port = packet.getPort();
        this.node = n;
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
                response = this.node.getPredecessor().toString();
                break;
            case "":
                break;
            case "CLP_FINGER":
                response = this.node.closestPrecedingFinger(Integer.parseInt(message[1])).toString();
                break;
            case "ONLINE":
                response = "TRUE";
                break;
        }

        if (response == null)
            response = "EMPTY";

        byte[] buf = response.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String cleanString(byte[] info) {
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