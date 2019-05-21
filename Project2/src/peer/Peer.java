package peer;

import middleware.RequestListener;
import storage.StorageManager;

import java.io.IOException;

public class Peer {
    // TODO Identify each peer by their certificate?
    public static final String PEER_ID = "tempID";

    public static void main(String[] args) {
        try {
            StorageManager.initStorage();
            new Thread(new RequestListener(Integer.parseInt(args[0]))).start();
            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
