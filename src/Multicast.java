import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Multicast {

    public static Integer PACKET_SIZE = 65000;

    private String address;

    private Integer port;

    private InetAddress group;

    private MulticastSocket socket;

    private ThreadPoolExecutor executer;

    /**
     * Default constructor
     * @param address
     * @param port
     */
    public Multicast(String address, String port)  {
        this.address = address;
        this.port = Integer.parseInt(port);

        try {
            this.group = InetAddress.getByName(this.address);
            this.socket = new MulticastSocket(this.port);
            this.socket.joinGroup(this.group);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.executer = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public ThreadPoolExecutor getExecuter() {
        return executer;
    }

    public MulticastSocket getSocket() {
        return socket;
    }

    public void send_packet(DatagramPacket packet) {
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DatagramPacket receive_packet() {
        byte[] buf = new byte[PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            this.socket.receive(packet);
        } catch (IOException e) {
            System.out.println("MULTICAST: Receive packet exception: " + e.toString());
        }

        return packet;
    }
}
