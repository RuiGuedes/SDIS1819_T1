package ReceiverThread;

import Peer.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MC extends Thread {

    private String address;

    private int port;

    private InetAddress group;

    /**
     * Default constructor
     * @param address
     * @param port
     */
    public MC(String address, String port) {
        this.address = address;
        this.port = Integer.parseInt(port);
        try {
            this.group = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        MulticastSocket mc_socket = Peer.create_socket(port, group);

    }
}
