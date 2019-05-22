package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Responsible for establishing a connection between the client and its local peer.
 *
 * In a connection, the peer will interpret the following commands:
 *
 * - BACKUP (-s | --share)? `filePath`
 *     The peer backs up the given file in its network.
 *
 *     When the -s / --share option is given, a metadata file is generated, which can be shared to other users so
 *     they can download the file
 *
 * - DONWLOAD (`metaPath`|`ownerPath`)
 *     The Peer downloads the file from the network, given its metadata file or owner file.
 *
 * - LIST (-o | --owner)
 *     The Peer lists the files it has backed up to the client.
 *
 * - LIST (-c | --chunk)
 *     The Peer lists the chunks it is storing for the network to the client.
 *
 *  TODO - STORAGE
 *  TODO - DELETE
 */
public class Connection {
    /**
     * Connects to the local peer and outputs the result of a single command
     *
     * @param args Command being sent to the peer
     */
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
