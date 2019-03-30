import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Task {

    protected Multicast M;

    public Task(Multicast M) {
        this.M = M;
    }
}

class PutChunk extends Task implements Runnable {

    private Multicast MDB;

    private Message message;

    private DatagramPacket packet;

    public PutChunk(Multicast MC, Multicast MDB, Message message, DatagramPacket packet) {
        super(MC);
        this.MDB = MDB;
        this.message = message;
        this.packet = packet;
    }

    public static String[] clean_array(String[] list) {
        List<String> cleaned = new ArrayList<>();

        for (String s: list) {
            if (s.length() > 0)
                cleaned.add(s);
        }

        return cleaned.toArray(new String[0]);
    }

    @Override
    public void run() {
        Timer timer = new Timer();

        for (int j = 1; j <= 5; j++) {
            MDB.send_packet(packet);
            final int[] stored_count = {0};

            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    DatagramPacket received_packet = M.receive_packet();
                    
                    String answer = received_packet.getData().toString();
                    String[] fields = answer.split(" ");
                    fields = clean_array(fields);
                    if (fields[0] == "STORED" && fields[1] == "1.0" &&
                            Integer.parseInt(fields[2]) == message.getServer_id() &&
                            fields[3] == message.getFile_id() &&
                            Integer.parseInt(fields[4]) == message.getChunk_no() &&
                            fields[5] == "\r\n\r\n")
                        stored_count[0]++;
                }
            }, j*1000);

            if (stored_count[0] >= message.getReplication_degree())
                break;
        }

        timer.cancel();
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
                Message response = new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no(), null);
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
