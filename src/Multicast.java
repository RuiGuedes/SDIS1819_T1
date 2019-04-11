import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class Multicast {

    /**
     * Multicast port
     */
    private Integer port;

    /**
     * Multicast group
     */
    private InetAddress group;

    /**
     * Multicast associated socket
     */
    private MulticastSocket socket;

    /**
     * Multicast thread pool executor
     */
    private ThreadPoolExecutor executor;

    /**
     * Default constructor
     * @param address Multicast address
     * @param port Port number
     */
    Multicast(String address, String port)  {
        this.port = Integer.parseInt(port);

        try {
            this.group = InetAddress.getByName(address);
            this.socket = new MulticastSocket(this.port);
            this.socket.joinGroup(this.group);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(500);
    }

    /**
     * Get's multicast executor
     */
    ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * Get's multicast group
     */
    InetAddress getGroup() {
        return group;
    }

    /**
     * Get's multicast port
     */
    Integer getPort() {
        return port;
    }

    /**
     * Sends packet to multicast group
     * @param packet Packet to be sent
     */
    void send_packet(DatagramPacket packet) {
        boolean flag = false;

        while(!flag) {
            try {                     
                this.socket.send(packet);
                flag = true;
            } catch (IOException ignored) {}
        }
    }

    /**
     * Another version of send_packet but receiving a Message argument
     * @param message Message to be converted into datagram packet and afterwards sent
     */
    void send_packet(Message message) {
        byte[] buf;
        DatagramPacket packet;

        buf = message.get_data();
        packet = new DatagramPacket(buf, buf.length, this.group, this.port);
        this.send_packet(packet);
    }

    /**
     * Receives packet and returns it
     * @return Received packet
     */
    DatagramPacket receive_packet() {
        byte[] buf = new byte[Message.MESSAGE_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {        
            this.socket.receive(packet);
        } catch (IOException e) {
            System.out.println("MULTICAST: Receive packet exception: " + e.toString());
        }

        return packet;
    }
}
