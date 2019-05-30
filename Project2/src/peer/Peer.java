package peer;

import chord.Chord;
import middleware.ChunkTransfer;
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

    private static Path rootPath;

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
            PEER_ID = InetAddress.getLocalHost().toString() + ":" + args[0];
            rootPath = Paths.get("./" + PEER_ID.split("/")[0] + args[0]);

            if(!chord.initialize(args)) {
                System.out.println("FAILED");
                System.exit(2);
            }

            ChunkStorage.init();
            OwnerStorage.init();

            new Thread(new RequestListener(Integer.parseInt(args[args.length - 1]))).start();
            new Thread(new ChunkTransfer(Integer.parseInt(args[0]))).start();

            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Peer " + PEER_ID + " failed to initialize!");
        }
    }


    public static String getPeerId() {
        return PEER_ID;
    }

    public static Path getRootPath() {
        return rootPath;
    }
}
