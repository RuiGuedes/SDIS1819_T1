import java.net.DatagramPacket;

public class Task {

    protected Multicast M;

    public Task(Multicast M) {
        this.M = M;
    }
}

class PutChunk extends Task implements Runnable {

    private Multicast MDB;

    public PutChunk(Multicast MC, Multicast MDB) {
        super(MC);
        this.MDB = MDB;
    }

    @Override
    public void run() {

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
    public Listener(Multicast M1, Multicast M2, Multicast MDR) {
        super(M1);
        this.M2 = M2;
        this.MDR = MDR;
    }

    @Override
    public void run() {
        while(true) {
            DatagramPacket packet = M.receive_packet();

            Message message = new Message(packet.getData().toString());
            decrypt_message(message);
        }
    }

    private void decrypt_message(Message message) {

        switch (message.getMessage_type()) {
            case "PUTCHUNK":
                // store Chunk
                // Send STORE message
                Message response = new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no());
                M2.send_packet(response);
            case "STORED":
                // Increment stored
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
