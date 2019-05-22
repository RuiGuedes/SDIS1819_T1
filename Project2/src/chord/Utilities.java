package chord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {

    public static void main(String[] args) throws UnknownHostException {
        System.out.println(hashCode(InetAddress.getLocalHost().getHostAddress(),9000));
        System.out.println(hashCode(InetAddress.getLocalHost().getHostAddress(),9001));
        System.out.println(hashCode("192.193.29.89",9003));
        System.out.println(hashCode("192.193.29.89",9034));
        System.out.println(hashCode("172.23.269.89",9000));
        System.out.println(hashCode("132.173.29.89",9000));
        System.out.println(hashCode("42.197.239.89",9004));
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
}
