package peer;

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

    /**
     * Initializes a peer
     *
     * @param args arguments for initializing the peer
     */
    public static void main(String[] args) {
        try {
            ChunkStorage.init();
            OwnerStorage.init();

            new Thread(new RequestListener(Integer.parseInt(args[0]))).start();

            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Peer " + PEER_ID + " failed to initialize!");
        }
    }
}
