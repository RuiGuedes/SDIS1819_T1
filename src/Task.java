import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.TimeUnit;

class Task {

    Multicast M;

    Task(Multicast M) {
        this.M = M;
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

    static String[] clean_array(String[] list) {
        List<String> cleaned = new ArrayList<>();

        for (String s: list) {
            if (s.length() > 0)
                cleaned.add(s);
        }

        return cleaned.toArray(new String[0]);
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

class Listener extends Task implements Runnable {

    /**
     * Multicast channel to send to: MC or MDB
     */
    private Multicast M2;

    /**
     * Multicast used for restore
     */
    private Multicast MDR;

    /**
     * Constructor of Listener class
     * @param M1 Multicast channel to listen to: MC or MDB
     * @param M2 Multicast channel to send to: MC or MDB
     * @param MDR Multicast used for restore
     */
    Listener(Multicast M1, Multicast M2, Multicast MDR) {
        super(M1);
        this.M2 = M2;
        this.MDR = MDR;
    }

    @Override
    public void run() {
        while(true) {
            // TODO Make listener channel execute thread to decrypt message
            decrypt_message(new Message(Arrays.toString(M.receive_packet().getData())));
        }
    }

    private void decrypt_message(Message message) {
        switch (message.getMessage_type()) {
            case "PUTCHUNK":
                // CHECK IF PEER IS NOT THE ONE SENDING PUT CHUNK !
                // store Chunk
                // Send STORE message
                Message response = new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no(), null);
                M2.send_packet(response);
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
