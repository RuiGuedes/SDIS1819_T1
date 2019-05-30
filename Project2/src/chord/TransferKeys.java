package chord;

import middleware.ChunkTransfer;
import storage.ChunkStorage;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * TransferKeys class
 */
public class TransferKeys extends Thread {

    /**
     * Associated node
     */
    private Node node;

    /**
     * Old predecessor of the node
     */
    private CustomInetAddress oldPredecessor;

    /**
     * Online status: True if online, false if offline
     */
    private boolean online;

    /**
     * TransferKeys class constructor
     * @param node Associated node
     */
    public TransferKeys(Node node) {
        this.node = node;
        this.oldPredecessor = this.node.getPredecessor();
        this.online = true;
    }

    @Override
    public void run() {
        while (this.online) {

            try {
                if (this.node.getPredecessor() != this.oldPredecessor) {
                    this.findKeysToTransfer(this.node.getPredecessor());
                    this.oldPredecessor = this.node.getPredecessor();
                }

                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Find the keys needed transfer for the new predecessor
     * @param newPredecessor New predecessor address
     */
    private void findKeysToTransfer(CustomInetAddress newPredecessor) throws InterruptedException, ExecutionException,
            IOException {
        long newPredecessorId = newPredecessor.getNodeID();
        long oldPredecessorId = this.oldPredecessor.getNodeID();
        String[] chunkIds;

        chunkIds = ChunkStorage.listFiles().split(System.lineSeparator());
        for (int i = 0 ; i < chunkIds.length - 1 ; i++) {
            String chunkId = chunkIds[i].split("\t")[0];

            if (Utilities.belongsToInterval(Long.parseLong(chunkId), oldPredecessorId, newPredecessorId))
                ChunkTransfer.transmitChunk(newPredecessor, ChunkStorage.get(chunkIds[i]));
        }
    }

    /**
     * Set online status to false
     */
    void terminate() { this.online = false; }
}
