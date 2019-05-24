package peer;

import chord.Chord;
import middleware.RequestListener;
import storage.ChunkStorage;
import storage.OwnerStorage;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Responsible for initializing a peer
 */
public class Peer {
    private static String PEER_ID;

    public static final Path rootPath = Paths.get("./peer");

    public static Chord chord = new Chord();

    /**
     * Initializes a peer
     *
     * @param args arguments for initializing the peer
     */
    public static void main(String[] args) {
        // Validate arguments
        if(args.length < 2 || args.length > 3) {
            System.out.println(
                    "Usage: java Peer [options] <PORT> (<PEER_CONTACT_ADDRESS:PEER_CONTACT_PORT>)? <CLIENT-PORT>"
            );
            System.exit(1);
        }

        try {
            PEER_ID = InetAddress.getLocalHost().toString();

            if(!chord.initialize(args)) {
                System.out.println("FAILED");
            }

            ChunkStorage.init();
            OwnerStorage.init();

            new Thread(new RequestListener(Integer.parseInt(args[args.length - 1]))).start();

            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Peer " + PEER_ID + " failed to initialize!");
        }
    }


    public static String getPeerId() {
        return PEER_ID;
    }
}
