package chord;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {

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

    static long fingerTableIthEntry(long nodeID, int key) {
        return (long)((nodeID + Math.pow(2,(key - 1))) % Math.pow(2, Chord.M));
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

    /**
     * Send a request and decode the answer returning address
     * @param inet Address to send request
     * @param request Request
     * @return Custom InetAddress
     */
    static CustomInetAddress addressRequest(CustomInetAddress inet, String request) {
        String response = sendRequest(inet, request);

        if(response == null)
            return null;

        if(response.equals("EMPTY"))
            return inet;

        String[] info = response.split(":");

        return new CustomInetAddress(info[0], Integer.parseInt(info[1]));
    }

    /**
     * Send a request and receive the answer returning them
     * @param inet Address to send the request
     * @param request Request
     * @return Received answer
     */
    static String sendRequest(CustomInetAddress inet, String request) {

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

    /**
     * Receive an answer of a request sent
     * @param socket Socket where the request has sent
     * @return Answer of the request
     */
    private static String receiveResponse(DatagramSocket socket) {
        if (socket == null)
            return null;

        byte[] buf = new byte[500];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.setSoTimeout(100);
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Receive packet exception: " + e.toString());
        }

        socket.close();

        return DecryptMessage.cleanString(packet.getData());
    }
}
