package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
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
 * - DOWNLOAD (`metaPath`|`ownerPath`)
 *     The Peer downloads the file from the network, given its metadata file or owner file.
 *
 * - LIST (-o | --owner)
 *     The Peer lists the files it has backed up to the client.
 *
 * - LIST (-c | --chunk)
 *     The Peer lists the chunks it is storing for the network to the client.
 *
 *  - DELETE `ownerPath`
 *      The Peer deletes the file from the network, given its owner file.
 *      
 *  - STORAGE `maxStorage`
 *      Sets the Maximum Storage used by the peer to store chunks. Any overflowing storage will be reclaimed.
 */
public class Connection {
    private static String maxStorage;
    private static String storageSize;

    /**
     * Connects to the local peer and outputs the result of a single command
     *
     * @param args Space split values to be used (Port then the command)
     */
    public static void main(String[] args) {
        if (args.length < 1)  {
            System.out.println("No command sent!");
            return;
        }

        try {
            final BufferedReader in = connectAndSend(args);

            String inputLine = in.readLine();
            while (inputLine != null) {
                System.out.println(inputLine);
                inputLine = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to the peer and sends a command
     *
     * @param args Space split values to be used (Port then the command)
     *
     * @return OutputStream for processing the peer response
     *
     * @throws IOException on error connecting to the peer
     */
    private static BufferedReader connectAndSend(String[] args) throws IOException {
        final SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        final SSLSocket socket = (SSLSocket) socketFactory.createSocket("localhost", Integer.parseInt(args[0]));
        socket.setEnabledCipherSuites(new String[] {
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        });

        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println(String.join("|", Arrays.copyOfRange(args, 1, args.length)));
        return in;
    }

    /**
     * Retrieves the Owner Files on the peer
     *
     * @param port Port where the peer is listening on
     *
     * @return List of Owner Files
     */
    static String[][] getFiles(String port) {
        ArrayList<String[]> files = new ArrayList<>();

        try {
            final BufferedReader in = connectAndSend(new String[]{port, "LIST", "-o"});

            String inputLine = in.readLine();
            while (inputLine != null) {
                final String[] ownerSplit = inputLine.split("\\t");
                files.add(new String[]{ownerSplit[1], ownerSplit[2]});

                inputLine = in.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return new String[0][2];
        }

        return files.toArray(new String[0][2]);
    }

    /**
     * Retrieves the chunk files on the peer
     *
     * @param port Port where the peer is listening on
     *
     * @return List of chunk files
     */
    static String[][] getChunks(String port) {
        ArrayList<String[]> chunks = new ArrayList<>();

        try {
            final BufferedReader in = connectAndSend(new String[]{port, "LIST", "-c"});

            String inputLine = in.readLine();
            while (inputLine != null) {
                chunks.add(inputLine.split("\\t"));
                inputLine = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0][2];
        }

        storageSize = chunks.remove(chunks.size() - 1)[0].split(": ")[1];
        maxStorage = chunks.remove(chunks.size() - 1)[0].split(": ")[1];

        return chunks.toArray(new String[0][2]);
    }

    /**
     * @return Peer's max storage in Bytes
     */
    static String getMaxStorage() {
        return maxStorage;
    }

    /**
     * @return Peer's used storage in Bytes
     */
    static String getStorageSize() {
        return storageSize;
    }
}
