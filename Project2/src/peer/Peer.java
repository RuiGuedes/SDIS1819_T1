package peer;

import chord.Chord;
import chord.Node;
import middleware.RequestListener;
import storage.StorageManager;

import java.io.IOException;

public class Peer {
    // TODO Identify each peer by their certificate?
    public static final String PEER_ID = "tempID";

    private static Chord chord = new Chord();

    public static void main(String[] args) {

        // Validate arguments
        if(args.length < 1 || args.length > 2) {
            System.out.println("Usage: java Peer [options] <PORT> <PEER_CONTACT_ADDRESS:PEER_CONTACT_PORT");
            System.exit(1);
        }

        try {
            if(!chord.initialize(args)) {
                System.out.println();
            }
            System.in.read();
//            StorageManager.initStorage();
//            new Thread(new RequestListener(Integer.parseInt(args[0]))).start();
//            System.out.println("Peer " + PEER_ID + " Online!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
