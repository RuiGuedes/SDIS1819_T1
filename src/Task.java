import java.net.DatagramPacket;
import java.util.Arrays;
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
            DatagramPacket packet = this.M.receive_packet();
            Message message = new Message(Message.decrypt_packet(Arrays.copyOf(packet.getData(), packet.getLength())));
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
                Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),1);
                break;
            case "GETCHUNK":
                Storage.exists_chunk(message.get_file_id(), message.get_chunk_no());
                // If so, send chunk
            case "CHUNK":
                // Save the chunk
            case "DELETE":
                /*Storage.exists_file(message.get_file_id());
                // If so, delete chunk
                Storage.delete_file(message.get_file_id());*/
            case "REMOVED":
                Storage.exists_chunk(message.get_file_id(), message.get_chunk_no());
                // Decrease count replication degree
                if (!(Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),-1))) {
                    Message new_putchunk = new Message("PUTCHUNK", Peer.get_protocol_version(),
                            Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), 1);//replication degree
                    byte[] chunk = Storage.read_chunk(message.get_file_id(), message.get_chunk_no());
                    /*byte[] data = new byte[new_putchunk.get_header().getBytes().length + .length];
                    System.arraycopy(message.get_header().getBytes(), 0, data, 0, message.get_header().getBytes().length);
                    System.arraycopy(chunk, 0, data, message.get_header().getBytes().length, chunk.length);

                    DatagramPacket packet = new DatagramPacket(data, data.length, MDB.getGroup(), MDB.getPort());
                    MDB.getExecuter().execute(new PutChunk(message, packet));*/
                }
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