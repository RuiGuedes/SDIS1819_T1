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

    private Multicast M2;

    private String channel;

    public Listener(Multicast M1, Multicast M2, String channel) {
        super(M1);
        this.M2 = M2;
        this.channel = channel;
    }

    @Override
    public void run() {
        DatagramPacket packet;

        while(true) {
            packet = M.receive_packet();

            Message message = new Message(packet.getData().toString());
            decrypt_message(message);
        }

    }

    private void decrypt_message(Message message) {
        Message answer;

        switch (message.getMessage_type()) {
            case "PUTCHUNK":
                // store Chunk
                // Send STORE message
                answer = new Message("STORED", message.getProtocol_version(), message.getServer_id(), message.getFile_id(), message.getChunk_no());
                send_answer(answer);
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

    // se calhar pode ser a send_packet da classe multicast a fazer isto
    private void send_answer(Message answer) {

        byte[] buf;
        DatagramPacket packet;

        buf = answer.get_full_header(new Integer[0]).getBytes();
        packet = new DatagramPacket(buf, buf.length, M2.getGroup(), M2.getPort());
        M2.send_packet(packet);
    }
}
