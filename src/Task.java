import java.awt.event.PaintEvent;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;
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
                System.out.println("PUTCHUNK");
                //if (Peer.getStorage().get_free_space() >= (message.get_body().length + 3)) {
                    Storage.create_chunk_info(message.get_file_id(), message.get_chunk_no(), message.get_replication_degree());
                    Storage.store_chunk(message.get_file_id(), message.get_chunk_no(), message.get_body());
                    Peer.getMC().send_packet(new Message("STORED", Peer.get_protocol_version(), Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), null, null));
                //}
                break;
            case "STORED":
                System.out.println("STORED");
                Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),1);
                break;
            case "GETCHUNK":
                if (Peer.get_server_id()== message.get_server_id())
                    break;

                if (Storage.exists_chunk(message.get_file_id(), message.get_chunk_no())) {
                    Message chunk_message = new Message("CHUNK", Peer.get_protocol_version(),
                            Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), null,
                            Storage.read_chunk(message.get_file_id(), message.get_chunk_no()));

                    try {
                        Thread.sleep(new Random().nextInt(401));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // TODO: check if is correct to check if receive a CHUNK message before send
                    Message received = new Message(Message.decrypt_packet(Peer.getMDR().receive_packet().getData()));
                    if (received.get_file_id() != message.get_file_id() && received.get_chunk_no() != message.get_chunk_no())
                        Peer.getMDR().send_packet(chunk_message);
                }
                break;
            case "CHUNK":
                if (Peer.get_server_id() == message.get_server_id())
                    break;
                // Save the chunk
                break;
            case "DELETE":
                if(Storage.exists_file(message.get_file_id()))
                    Storage.delete_file(message.get_file_id());
                break;
            case "REMOVED":
                if (Peer.get_server_id()== message.get_server_id())
                    break;

                Storage.exists_chunk(message.get_file_id(), message.get_chunk_no());
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
                }
                break;
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
     * Thread termination status: true = success : false = failed
     */
    private Boolean termination_status;

    /**
     * Determines whether thread exited run method
     */
    private Boolean exit_flag;

    /**
     * Put chunk constructor
     */
    PutChunk(Message message, DatagramPacket packet) {
        this.message = message;
        this.packet = packet;
        this.termination_status = false;
        this.exit_flag = false;
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            if(this.exit_flag)
                break;

            Storage.create_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), this.message.get_replication_degree());
            Peer.getMDB().send_packet(packet);

            try {
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Storage.read_chunk_info(message.get_file_id(), message.get_chunk_no()) >= message.get_replication_degree()) {
                this.termination_status = true;
                break;
            }
        }

        this.exit_flag = true;
    }

    /**
     * Returns thread exit status
     */
    boolean get_termination_status() {
        return termination_status;
    }

    /**
     * Checks whether thread is running or not
     */
    boolean is_running() {
        return !this.exit_flag;
    }

    /**
     * Terminates this thread by activating exit_flag
     */
    void terminate() {
        this.exit_flag = true;
    }


}