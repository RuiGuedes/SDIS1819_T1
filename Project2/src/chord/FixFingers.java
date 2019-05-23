package chord;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * FixFingers class
 */
public class FixFingers extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * Fix fingers class constructor
     * @param node Associated node
     */
    FixFingers(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {

        // Random seeder
        Random rand = new Random();

        while (online) {
            // Generates random key to be updated in the respective finger table
            int key = rand.nextInt(Chord.M - 1) + 2;

            // Updates the finger table with a certain key
            this.node.setIthFinger(key, this.node.findSuccessor(Utilities.fingerTableIthEntry(this.node.getID(), key)));

            try {
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Terminates thread
     */
    public void terminate() {
        this.online = false;
    }

}
