package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;

public class Connection {

    public static void main(String[] args) {
        try {
            final SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            final SSLSocket socket = (SSLSocket) socketFactory.createSocket("localhost", Integer.parseInt(args[0]));

            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println(in.readLine());
            out.println(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
