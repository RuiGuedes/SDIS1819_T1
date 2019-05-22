package chord;

import java.net.InetAddress;

class CustomInetAddress {

    /**
     * Peer InetAddress
     */
    private InetAddress address;

    /**
     * Peer associated port
     */
    private Integer port;

    /**
     * CustomInetAddress constructor
     * @param address Address used for creating InetAddress
     * @param port Port associated to the peer/node
     */
    CustomInetAddress(String address, Integer port) {
        this.address = Utilities.createInetAddress(address);
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof CustomInetAddress))
            return false;

        CustomInetAddress object = (CustomInetAddress) obj;

        return this.address.getHostAddress().equals(object.getHostAddress()) && this.port.equals(object.getPort());
    }

    /**
     * Returns InetAddress
     */
    InetAddress getAddress() {
        return address;
    }

    /**
     * Returns host address of the associated InetAddress
     */
    String getHostAddress() {
        return address.getHostAddress();
    }

    /**
     * Returns the port associated to peer/node
     */
    Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return this.getHostAddress() + ":" + this.port;
    }
}
