package chord;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {

    public static void main(String[] args) {

    }

    /**
     * Create Hash code with IP and Port number
     * @param ip IP address
     * @param port Port number
     * @return Hash Code
     */
    static long hashCode (String ip, int port) {
        String s = ip + port;

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (digest != null) {
            byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            long truncate = 0;

            for (int i = 0 ; i < Chord.M ; i += 4) {
                truncate |= ((hash[i] & 0xF) << i);
            }

            return truncate & 0xFFFFFFFFL;
        }

        return 0;
    }

    /**
     * Creates InetAddress given an address
     * @param address Address to be used
     * @return InetAddress
     */
    static InetAddress createInetAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if(inetAddress == null)
                throw new UnknownHostException();
            else
                return inetAddress;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves local host IP address
     * @return IP address
     */
    static String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    static CustomInetAddress sendRequest(CustomInetAddress inet, String request) {

        if (inet == null || request == null)
            return null;

        byte[] buf;
        DatagramSocket socket = null;
        DatagramPacket packet;

        buf = request.getBytes();
        packet = new DatagramPacket(buf, buf.length, inet.getAddress(), inet.getPort());

        try {
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return receiveResponse(socket);
    }

    private static CustomInetAddress receiveResponse(DatagramSocket socket) {
        if (socket == null)
            return null;

        byte[] buf = new byte[1000];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Receive packet exception: " + e.toString());
        }

        socket.close();

        String[] response = new String(packet.getData(), StandardCharsets.UTF_8).split(":");

        return new CustomInetAddress(response[0], Integer.parseInt(response[1]));
    }
}
