package Multicast;

import Peer;

import java.io.IOException;
import java.net.DatagramPacket;
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
        DatagramPacket packet;

        while(true) {
            System.out.println("adasdasdasd");
            byte[] buf = new byte[1000];
            packet = new DatagramPacket(buf, buf.length);
            try {
                mc_socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String message = new String(packet.getData());
            process_message(message);
        }
    }

    private void process_message(String message) {
        String[] fields = Peer.clean_array(message.split(" "));

        String version = fields[1];
        int sender_id = Integer.parseInt(fields[2]);
        String file_id = fields[3];
        int chunk_no = Integer.parseInt(fields[4]);

        switch (fields[0]) {
            case "STORED":
                break;
            case "GETCHUNK":
                break;
            case "DELETE":
                break;
            case "REMOVED":
                break;
            default:
                break;
        }

    }
}
