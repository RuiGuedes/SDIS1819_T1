import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Multicast {

    private String name;

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
    public Multicast(String name, String address, String port)  {
        this.name = name;
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

    public InetAddress getGroup() {
        return group;
    }

    public Integer getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public void send_packet(DatagramPacket packet) {
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send_packet(Message answer) {

        byte[] buf;
        DatagramPacket packet;

        buf = answer.get_header().getBytes();
        packet = new DatagramPacket(buf, buf.length, this.group, this.port);
        this.send_packet(packet);
    }

    public DatagramPacket receive_packet() {
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
