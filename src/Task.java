import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;


class Listener implements Runnable {

    /**
     * Channel associated to the task
     */
    private Multicast M;

    /**
     * Constructor of Listener class
     * @param M Multicast channel to listen to
     */
    Listener(Multicast M) {
        this.M = M;
    }

    @Override
    public void run() {
        while(true) {
            Message message = new Message(Message.bytes_to_string(this.M.receive_packet().getData()));
            this.M.getExecuter().execute(new DecryptMessage(message));
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
                if(Peer.getServerId() != message.getServer_id()) {
                    Storage.store_chunk(message.getFile_id(), message.getChunk_no(), message.getBody());
                    Peer.getMC().send_packet(new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no(), null));
                }
                break;
            case "STORED":
                Storage.store_count_messages(message.getFile_id(), message.getChunk_no(), message.getReplication_degree());
                break;
            case "GETCHUNK":
                // Check if you have the chunk  **
                // If so, send chunk
            case "CHUNK":
                // Save the chunk
            case "DELETE":
                // Check if you have the chunk  **
                // If so, delete chunk
                Storage.remove_chunk(message.getFile_id(), String.valueOf(message.getChunk_no()));
            case "REMOVED":
                // Check if you have the chunk  **
                // Decrease count replication degree
            default:
                System.out.println("Invalid message type: " + message.getMessage_type());
        }
    }
}

class PutChunk implements Runnable {

    /**
     * Message containing information to be sent
     */
    private Message message;

    /**
     * Packet to be sent to the MDB
     */
    private DatagramPacket packet;

    /**
     * Put chunk constructor
     */
    PutChunk(Message message, DatagramPacket packet) {
        this.message = message;
        this.packet = packet;
    }

    @Override
    public void run() {
//        int[] waiting_time = {1, 2, 4, 8, 16};

        for (int i = 0; i < 5; i++) {
            Peer.getMDB().send_packet(packet);

            try {
//                TimeUnit.SECONDS.sleep(waiting_time[i]);
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Storage.read_count_messages(message.getFile_id(), message.getChunk_no()) >= message.getReplication_degree())
                break;
        }
    }
}