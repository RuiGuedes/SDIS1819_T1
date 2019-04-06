import java.awt.event.PaintEvent;
import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.Callable;
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
                // TODO - Check why this (Commented lines) is wrong
                //if (Peer.getStorage().get_free_space() >= (message.get_body().length + Storage.CHUNK_INFO_SIZE)) {
                    Storage.create_chunk_info(message.get_file_id(), message.get_chunk_no(), message.get_replication_degree());
                    Storage.store_chunk(message.get_file_id(), message.get_chunk_no(), message.get_body());
                    Peer.getMC().send_packet(new Message("STORED", Peer.get_protocol_version(), Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), null, null));
                //}
                break;
            case "STORED":
                if(Storage.syncronized_contains_chunk_info(message.get_file_id(), message.get_chunk_no()))
                    Storage.syncronized_inc_chunk_info(message.get_file_id(), message.get_chunk_no());
                else
                    Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),1);
                break;
            case "GETCHUNK":
                Peer.getMDR().getExecuter().execute(new GetChunk(message));
                break;
            case "CHUNK":
                if(Storage.files_to_restore.containsKey(message.get_file_id()))
                    Storage.files_to_restore.put(message.get_file_id(), new HashMap<>());

                if(!Storage.files_to_restore.get(message.get_file_id()).containsKey(message.get_chunk_no()))
                    Storage.files_to_restore.get(message.get_file_id()).put(message.get_chunk_no(), message.get_body());
                break;
            case "DELETE":
                Storage.delete_file(message.get_file_id());
                break;
            case "REMOVED":
                /*Storage.exists_chunk(message.get_file_id(), message.get_chunk_no());
                // Decrease count replication degree
                if (!(Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),-1))) {
                    try {
                        Thread.sleep(new Random().nextInt(401));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!(Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),0))) {

                        Message new_putchunk = new Message("PUTCHUNK", Peer.get_protocol_version(),
                                Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(),
                                Storage.read_chunk_info_replication(message.get_file_id(), message.get_chunk_no()),
                                Storage.read_chunk(message.get_file_id(), message.get_chunk_no()));

                        byte[] data = new_putchunk.get_data();

                        DatagramPacket packet = new DatagramPacket(data, data.length, Peer.getMDB().getGroup(), Peer.getMDB().getPort());

                        Peer.getMDB().getExecuter().execute(new PutChunk(new_putchunk, packet));
                    }
                }*/
                break;
            default:
                System.out.println("Invalid message type: " + message.get_message_type());
        }
    }
}

class PutChunk implements Callable<Boolean> {

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
    public Boolean call() {

        int current_replication_degree = 0;

        for (int i = 0; i < 5; i++) {
            if (Storage.syncronized_contains_chunk_info(this.message.get_file_id(),this.message.get_chunk_no()))
                Storage.syncronized_put_chunk_info(this.message.get_file_id(),this.message.get_chunk_no(),0);

            Peer.getMDB().send_packet(packet);

            try {
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if((current_replication_degree = Storage.syncronized_get_chunk_info(this.message.get_file_id(), this.message.get_chunk_no())) >= this.message.get_replication_degree())
                break;
        }

        return current_replication_degree > 0;
    }

}

class GetChunk implements Runnable {

    private Message message;

    GetChunk(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        if (Storage.has_chunk(this.message.get_file_id(), this.message.get_chunk_no())) {

            Message chunk_message = new Message("CHUNK", Peer.get_protocol_version(),
                    Peer.get_server_id(), this.message.get_file_id(), this.message.get_chunk_no(), null,
                    Storage.read_chunk(this.message.get_file_id(), this.message.get_chunk_no()));

            try {
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(Storage.files_to_restore.containsKey(this.message.get_file_id())) {
                if (Storage.files_to_restore.get(this.message.get_file_id()).containsKey(this.message.get_chunk_no()))
                    Storage.files_to_restore.remove(this.message.get_file_id());
                else
                    Peer.getMDR().send_packet(chunk_message);
            }
            else
                Peer.getMDR().send_packet(chunk_message);
        }
    }
}