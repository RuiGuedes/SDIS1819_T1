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

        System.out.println("1: " + message);
    }

    @Override
    public void run() {
        byte[] buf;
        DatagramPacket response;
        switch(message[0]){
            case "FIND_SUCCESSOR":
                String successor = this.local.getFingerTable(1).toString();
                buf = successor.getBytes();
                response = new DatagramPacket(buf, buf.length, this.address, this.port);
                try {
                    this.socket.send(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "":

                break;
            default:

        }

    }
}
