import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class Task {

    /**
     * Channel associated to the task
     */
    Multicast M;

    /**
     * Task constructor
     * @param M Channel associated to the task
     */
    Task(Multicast M) {
        this.M = M;
    }
}

class Listener extends Task implements Runnable {

    /**
     * Constructor of Listener class
     * @param M Multicast channel to listen to
     */
    Listener(Multicast M) {
        super(M);
    }

    @Override
    public void run() {
        while(true) {
            Message message = new Message(Arrays.toString(M.receive_packet().getData()));
            M.getExecuter().execute(new DecryptMessage(message));
        }
    }
}

class DecryptMessage implements Runnable {

    /**
     * Message to be decrypted
     */
    private Message message;

    /**
     * Decrypt message constructor
     * @param message Message to be decrypted
     */
    DecryptMessage(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        switch (message.getMessage_type()) {
            case "PUTCHUNK":
                // CHECK IF PEER IS NOT THE ONE SENDING PUT CHUNK !
                // store Chunk
                // Send STORE message
                Message response = new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no(), null);
                Peer.getMC().send_packet(response);
            case "STORED":
                Storage.store_count_messages(message.getFile_id(), message.getChunk_no(), message.getReplication_degree());
            case "GETCHUNK":
                // Check if you have the chunk  **
                // If so, send chunk
            case "CHUNK":
                // Save the chunk
            case "DELETE":
                // Check if you have the chunk  **
                // If so, delete chunk
            case "REMOVED":
                // Check if you have the chunk  **
                // Decrease count replication degree
            default:
                System.out.println("Invalid message type: " + message.getMessage_type());
        }
    }
}

class PutChunk extends Task implements Runnable {

    private Multicast MDB;

    private Message message;

    private DatagramPacket packet;

    PutChunk(Multicast MC, Multicast MDB, Message message, DatagramPacket packet) {
        super(MC);
        this.MDB = MDB;
        this.message = message;
        this.packet = packet;
    }

    @Override
    public void run() {
        int[] waiting_time = {1, 2, 4, 8, 16};

        for (int i = 0; i < 5; i++) {
            MDB.send_packet(packet);

            try {
                TimeUnit.SECONDS.sleep(waiting_time[i]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Substitute 1 for function responsible for reading file
            if (1 >= message.getReplication_degree())
                break;
        }
    }
}