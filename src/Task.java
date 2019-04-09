import java.io.*;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
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
                if(Peer.get_protocol_version().equals("2.0")) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if ((Peer.getStorage().get_free_space() >= message.get_body().length) && !Storage.has_chunk(message.get_file_id(), message.get_chunk_no())) {
                    if(Peer.get_protocol_version().equals("2.0") && Synchronized.synchronized_get_stored_message(message.get_file_id(), message.get_chunk_no()) >= message.get_replication_degree())
                        break;

                    int current_replication = Peer.get_protocol_version().equals("2.0") ? Synchronized.synchronized_get_stored_message(message.get_file_id(), message.get_chunk_no()) : 0;

                    Storage.create_chunk_info(message.get_file_id(), message.get_chunk_no(), current_replication, message.get_replication_degree());
                    Storage.store_chunk(message.get_file_id(), message.get_chunk_no(), message.get_body());
                    Peer.getMC().send_packet(new Message("STORED", Peer.get_protocol_version(), Peer.get_server_id(), message.get_file_id(), message.get_chunk_no(), null, null));
                }
                else {
                    if(!Storage.putchunk_messages.containsKey(message.get_file_id())) {
                        Storage.putchunk_messages.put(message.get_file_id(), new HashMap<>());
                    }
                    Storage.putchunk_messages.get(message.get_file_id()).put(message.get_chunk_no(), true);
                }
                break;
            case "STORED":
                if(Synchronized.synchronized_contains_chunk_info(message.get_file_id(), message.get_chunk_no())) {
                    Synchronized.synchronized_inc_chunk_info(message.get_file_id(), message.get_chunk_no());
                }
                else {
                    if(Peer.get_protocol_version().equals("2.0") && !Storage.has_chunk_info(message.get_file_id(), message.get_chunk_no()))
                        Synchronized.synchronized_inc_stored_message(message.get_file_id(), message.get_chunk_no());
                    else
                        Storage.store_chunk_info(message.get_file_id(), message.get_chunk_no(),1);
                }
                break;
            case "GETCHUNK":
                Peer.getMDR().getExecuter().execute(new GetChunk(message));
                break;
            case "CHUNK":
                if(!Synchronized.synchronized_contains_files_to_restore(message.get_file_id(), null))
                    Synchronized.synchronized_put_files_to_restore(message.get_file_id(), null, null);

                if(!Synchronized.synchronized_contains_files_to_restore(message.get_file_id(), message.get_chunk_no()))
                    Synchronized.synchronized_put_files_to_restore(message.get_file_id(), message.get_chunk_no(), message.get_body());
                break;
            case "DELETE":
                Storage.delete_file(message.get_file_id());
                break;
            case "REMOVED":
                Peer.getMC().getExecuter().execute(new Removed(message));
                break;
            case "DELETEDFILES": // Protocol Version 2.0
                Peer.getMC().getExecuter().execute(new DeletedFiles(message));
                break;
            default:
                System.out.println("Unknown message type: " + message.get_message_type());
                break;
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
            if (!Synchronized.synchronized_contains_chunk_info(this.message.get_file_id(),this.message.get_chunk_no()))
                Synchronized.synchronized_put_chunk_info(this.message.get_file_id(),this.message.get_chunk_no(),0);

            if((current_replication_degree = Synchronized.synchronized_get_chunk_info(this.message.get_file_id(), this.message.get_chunk_no())) >= this.message.get_replication_degree())
                break;
            else
                Peer.getMDB().send_packet(packet);

            try {
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if((current_replication_degree = Synchronized.synchronized_get_chunk_info(this.message.get_file_id(), this.message.get_chunk_no())) >= this.message.get_replication_degree())
                break;
        }

        // If there exists a chunk that was not replicated
        if(current_replication_degree == 0) {
            synchronized (Peer.file_backup_status) {
                Peer.file_backup_status.put(message.get_file_id(), false);
            }
        }

        return true;
    }

}

class GetChunk implements Runnable {

    /**
     * Message containing information
     */
    private Message message;

    /**
     * Get chunk constructor
     */
    GetChunk(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        if (Storage.has_chunk(this.message.get_file_id(), this.message.get_chunk_no())) {

            byte[] chunk_body = Storage.read_chunk(this.message.get_file_id(), this.message.get_chunk_no());
            byte[] message_body = this.message.get_protocol_version().equals("2.0") && Peer.get_protocol_version().equals("2.0") ? null : chunk_body;

            Message chunk_message = new Message("CHUNK", Peer.get_protocol_version(),
                    Peer.get_server_id(), this.message.get_file_id(), this.message.get_chunk_no(), null,
                    message_body);

            try {
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(this.message.get_protocol_version().equals("2.0") && Peer.get_protocol_version().equals("2.0")) {
                try {
                    Socket client_socket = new Socket();

                    do {
                        if(Synchronized.synchronized_contains_files_to_restore(this.message.get_file_id(), null)) {
                            if (Synchronized.synchronized_contains_files_to_restore(this.message.get_file_id(), this.message.get_chunk_no())) {
                                Synchronized.synchronized_remove_files_to_restore(this.message.get_file_id());
                                return;
                            }
                        }

                        try {
                            client_socket = new Socket("DESKTOP-Q4SJA80", 4444);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    while (!client_socket.isConnected());

                    Peer.getMDR().send_packet(chunk_message);

                    try {
                        // Time used to synchronize actions
                        TimeUnit.MILLISECONDS.sleep(new Random().nextInt(15));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    OutputStream out_to_server = client_socket.getOutputStream();
                    out_to_server.write(chunk_message.get_chunk_no());
                    out_to_server.write(chunk_body);
                    client_socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                if(Synchronized.synchronized_contains_files_to_restore(this.message.get_file_id(), null)) {
                    if (Synchronized.synchronized_contains_files_to_restore(this.message.get_file_id(), this.message.get_chunk_no()))
                        Synchronized.synchronized_remove_files_to_restore(this.message.get_file_id());
                    else
                        Peer.getMDR().send_packet(chunk_message);
                }
                else
                    Peer.getMDR().send_packet(chunk_message);
            }

        }
    }
}

class Removed implements Runnable {

    /**
     * Message containing information
     */
    private Message message;

    /**
     * Removed constructor
     */
    Removed(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        // Checks if current peer has stored a certain chunk
        if(Storage.has_chunk(this.message.get_file_id(), this.message.get_chunk_no())) {
            // Decrease count replication degree
            if (!(Storage.store_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), -1))) {

                if(!Storage.putchunk_messages.containsKey(this.message.get_file_id()))
                    Storage.putchunk_messages.put(this.message.get_file_id(), new HashMap<>());

                try {
                    TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Check if a put chunk was received
                if(Storage.putchunk_messages.get(this.message.get_file_id()).containsKey(this.message.get_chunk_no())) {
                    Storage.putchunk_messages.get(this.message.get_file_id()).remove(this.message.get_chunk_no());
                    return;
                }

                // Checks if another peer has already backed up the given chunk
                if (!(Storage.store_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), 0))) {

                    Message putchunk_message = new Message("PUTCHUNK", Peer.get_protocol_version(),
                            Peer.get_server_id(), this.message.get_file_id(), this.message.get_chunk_no(),
                            Storage.read_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), 1),
                            Storage.read_chunk(this.message.get_file_id(), this.message.get_chunk_no()));

                    byte[] data = putchunk_message.get_data();
                    DatagramPacket packet = new DatagramPacket(data, data.length, Peer.getMDB().getGroup(), Peer.getMDB().getPort());

                    // Put Chunk backup protocol simplified
                    for (int i = 0; i < 5; i++) {
                        Peer.getMDB().send_packet(packet);

                        try {
                            TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(Storage.read_chunk_info(this.message.get_file_id(), this.message.get_chunk_no(), 0) >= putchunk_message.get_replication_degree())
                            break;
                    }
                }
            }
        }
    }
}

class DeletedFiles implements Runnable {

    /**
     * Message containing information
     */
    private int date;

    /**
     * DeletedFiles constructor
     */
    DeletedFiles(Message message) {
        this.date = message.get_chunk_no();
    }

    @Override
    public void run() {

        ArrayList<String> deleted_files = new ArrayList<>();

        // Get deleted files after a certain date
        for(Map.Entry<Long, String> file : Storage.deleted_files_log.entrySet()) {
            if(file.getKey() > date)
                deleted_files.add(file.getValue());
        }

        // Execute delete protocol
        for(String file : deleted_files) {
            Peer.getMC().send_packet(new Message("DELETE", Peer.get_protocol_version(), Peer.get_server_id(), file, null, null, null));
        }

    }
}

class ServerSocketThread implements Runnable {

    private String file_id;

    private Integer stored_chunks;

    private ServerSocket server_socket;

    ServerSocketThread(String file_id, Integer stored_chunks) {
        this.file_id = file_id;
        this.stored_chunks = stored_chunks;

        try {
            this.server_socket = new ServerSocket(4444);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        int current_chunks_stored = Synchronized.synchronized_size_files_to_restore(file_id);

        while (current_chunks_stored < this.stored_chunks || Synchronized.synchronized_contains_null_value(file_id)) {
            try {
                Socket connection_socket = this.server_socket.accept();

                InputStream in_from_client = connection_socket.getInputStream();

                int chunk_no = in_from_client.read();

                try {
                    // Time used to synchronize actions
                    TimeUnit.MILLISECONDS.sleep(new Random().nextInt(15));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Synchronized.synchronized_put_files_to_restore(file_id, chunk_no, in_from_client.readAllBytes());
                current_chunks_stored = Synchronized.synchronized_size_files_to_restore(file_id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}