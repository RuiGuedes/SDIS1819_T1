package Multicast;

import Peer;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MDB extends Thread {

    private String address;

    private int port;

    private InetAddress group;

    /**
     * Default constructor
     * @param address
     * @param port
     */
    public MDB(String address, String port) {
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

        MulticastSocket mdb_socket = Peer.create_socket(port, group);

    }

}
