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
        if (args.length < 1)  {
            System.out.println("No command sent!");
            return;
        }

        try {
            final SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            final SSLSocket socket = (SSLSocket) socketFactory.createSocket("localhost", Integer.parseInt(args[0]));

            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(String.join("|", Arrays.copyOfRange(args, 1, args.length)));

            String inputLine = in.readLine();
            while (inputLine != null) {
                System.out.println(inputLine);
                inputLine = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
