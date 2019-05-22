package peer;

import chord.Chord;
import chord.Node;
import middleware.RequestListener;
import storage.ChunkStorage;
import storage.OwnerStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Responsible for initializing a peer
 */
public class Peer {
    // TODO Identify each peer by their certificate?
    public static final String PEER_ID = "tempID";

    public static final Path rootPath = Paths.get("./peer");

    public static Chord chord = new Chord();

    /**
     * Initializes a peer
     *
     * @param args arguments for initializing the peer
     */
    public static void main(String[] args) {

        // Validate arguments
        if(args.length < 1 || args.length > 2) {
            System.out.println("Usage: java Peer [options] <PORT> <PEER_CONTACT_ADDRESS:PEER_CONTACT_PORT");
            System.exit(1);
        }

        try {
            if(!chord.initialize(args)) {
                System.out.println("FAILED");
            }

            System.in.read();
//            ChunkStorage.init();
//            OwnerStorage.init();
//
//            new Thread(new RequestListener(Integer.parseInt(args[0]))).start();
//
//            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Peer " + PEER_ID + " failed to initialize!");
        }
    }


}
