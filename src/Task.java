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
            Message message = new Message(Message.decrypt_packet(this.M.receive_packet().getData()));
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
        if(Peer.get_server_id().equals(message.get_server_id()))
            return;

        switch (message.get_message_type()) {
            case "PUTCHUNK":
                // TODO - Check if there is space available
                Storage.create_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), this.message.get_replication_degree());
                Storage.store_chunk(message.get_file_id(), message.get_chunk_no(), message.get_body());
                Peer.getMC().send_packet(new Message("STORED", Peer.get_protocol_version(), Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), null));
                break;
            case "STORED":
                Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no());
                break;
            case "GETCHUNK":
                // Check if you have the chunk  **
                Storage.exists_chunk(message.getFile_id(), message.getChunk_no());
                // If so, send chunk
            case "CHUNK":
                // Save the chunk
            case "DELETE":
                // Check if you have the chunk  **
                Storage.exists_chunk(message.getFile_id(), message.getChunk_no());
                // If so, delete chunk
                Storage.delete_chunk(message.getFile_id(), message.getChunk_no());
            case "REMOVED":
                // Check if you have the chunk  **
                Storage.exists_chunk(message.getFile_id(), message.getChunk_no());
                // Decrease count replication degree
                Storage.
            default:
                System.out.println("Invalid message type: " + message.get_message_type());
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

        for (int i = 0; i < 5; i++) {
            Storage.create_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), this.message.get_replication_degree());
            Peer.getMDB().send_packet(packet);

            try {
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Storage.read_chunk_info(message.get_file_id(), message.get_chunk_no()) >= message.get_replication_degree())
                break;
        }
    }
}