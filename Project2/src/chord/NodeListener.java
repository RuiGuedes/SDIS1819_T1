package chord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class NodeListener extends Thread {

    private Node local;
    private DatagramSocket socket = null;
    private boolean terminate = false;

    NodeListener(Node n) {
        this.local = n;
        CustomInetAddress address = n.getPeerAddress();

        try {
            this.socket = new DatagramSocket(address.getPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(!terminate) {
            byte[] buf = new byte[1000];
            DatagramPacket receive = new DatagramPacket(buf, buf.length);

            try {
                this.socket.receive(receive);
                new Thread( new DecryptMessage(this.socket, receive, this.local)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Terminate the thread
     */
    public void terminate() {
        this.terminate = false;
    }
}

class DecryptMessage extends Thread {

    private String[] message;
    private InetAddress address;
    private Integer port;
    private DatagramSocket socket;
    private Node local;

    DecryptMessage(DatagramSocket socket, DatagramPacket packet, Node n) {
        this.socket = socket;
        this.message = new String(packet.getData(), StandardCharsets.UTF_8).split(":");
        this.address = packet.getAddress();
        this.port = packet.getPort();
        this.local = n;
    }

    @Override
    public void run() {
        String response = null;

        switch(message[0]){
            case "FIND_SUCCESSOR":
                response = this.local.findSuccessor(Integer.parseInt(message[1])).toString();
                break;
            case "CLP_FINGER":
                response = this.local.closestPrecedingFinger(Integer.parseInt(message[1])).toString();
                break;
            case "YOUR_SUCCESSOR":
                response = this.local.getSuccessor().toString();
                break;
            case "ONLINE":
                response = "TRUE";
                break;
        }

        byte[] buf = response.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
