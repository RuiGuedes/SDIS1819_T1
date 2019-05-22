package chord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {
    private static int M = 16;

    public static void main(String[] args) throws UnknownHostException {
        System.out.println(hashCode(InetAddress.getLocalHost().getHostAddress(),9000));
        System.out.println(hashCode(InetAddress.getLocalHost().getHostAddress(),9001));
        System.out.println(hashCode("192.193.29.89",9003));
        System.out.println(hashCode("192.193.29.89",9034));
        System.out.println(hashCode("172.23.269.89",9000));
        System.out.println(hashCode("132.173.29.89",9000));
        System.out.println(hashCode("42.197.239.89",9004));
    }

    public static long hashCode (String ip, int port) {
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

            for (int i = 0 ; i < M/4 ; i ++) {
                truncate |= ((hash[i] & 0xF) << i*4);
            }

            return truncate & 0xFFFFFFFFL;
        }

        return 0;
    }
}
