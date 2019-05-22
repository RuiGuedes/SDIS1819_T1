package chord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NodeListener extends Thread {

    private Node local;
    private DatagramSocket socket = null;

    public NodeListener(Node n) {
        this.local = n;
        CustomInetAddress address = n.getPeerAddress();

        try {
            this.socket = new DatagramSocket(address.getPort(), address.getAddress());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
            byte[] buf = new byte[1000];
            DatagramPacket receive = new DatagramPacket(buf, buf.length);

            try {
                this.socket.receive(receive);
                new Thread( new DecryptMessage(receive)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class DecryptMessage extends Thread {

    private DatagramPacket message;

    DecryptMessage(DatagramPacket message) {
        this.message = message;
    }

    @Override
    public void run() {

    }
}
