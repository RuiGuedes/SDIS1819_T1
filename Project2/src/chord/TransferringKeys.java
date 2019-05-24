package chord;

import storage.ChunkStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * TransferringKeys class
 */
public class TransferringKeys extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Old predecessor of the node
     */
    private CustomInetAddress oldPredecessor;

    /**
     * List of Keys to transfer for the new predecessor
     */
    private ArrayList<Long> keysToTransfer;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * TransferringKeys class constructor
     * @param node Associated node
     */
    public TransferringKeys(Node node) {
        this.node = node;
        this.oldPredecessor = this.node.getPredecessor();
        this.keysToTransfer = null;
        this.online = true;
    }

    @Override
    public void run() {
        while(this.online) {
            if (this.node.getPredecessor() != this.oldPredecessor) {
                this.findKeysToTransfer(this.node.getPredecessor());
                this.oldPredecessor = this.node.getPredecessor();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Find the keys needed transfer for the new predecessor
     * @param newPredecessor New predecessor address
     */
    private void findKeysToTransfer(CustomInetAddress newPredecessor) {
        long newPredecessorId = newPredecessor.getNodeID();
        long oldPredecessorId = this.oldPredecessor.getNodeID();
        String[] chunkIds;

        try {
            chunkIds = ChunkStorage.listFiles().split(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0 ; i < chunkIds.length - 1 ; i++) {
            long numId = Long.parseLong(chunkIds[i].split("\t")[0]);

            if(Utilities.belongsToInterval(numId,oldPredecessorId, newPredecessorId))
                this.keysToTransfer.add(numId);
        }
    }

    /**
     * Get the keys to be transferred to the new predecessor
     * @return Keys array
     */
    public ArrayList<Long> getKeysToTransfer() { return keysToTransfer; }

    /**
     * Set online status to false
     */
    void terminate() { this.online = false; }
}
