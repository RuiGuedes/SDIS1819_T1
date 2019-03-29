import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
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
}
