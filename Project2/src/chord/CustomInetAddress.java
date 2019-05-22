package chord;

import java.net.InetAddress;

public class CustomInetAddress {

    private InetAddress address;

    private Integer port;

    public CustomInetAddress(String address, Integer port) {
        this.address = Chord.createInetAddress(address);
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }
    
}
