import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Multicast {

    private String address;

    private int port;

    private InetAddress group;

    private MulticastSocket socket;

    private ThreadPoolExecutor executer;

    /**
     * Default constructor
     * @param address
     * @param port
     */
    public Multicast(String address, String port) throws IOException {
        this.address = address;
        this.port = Integer.parseInt(port);
        this.group = InetAddress.getByName(this.address);

        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);

        this.executer = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public ThreadPoolExecutor getExecuter() {
        return executer;
    }

    public MulticastSocket getSocket() {
        return socket;
    }
}
