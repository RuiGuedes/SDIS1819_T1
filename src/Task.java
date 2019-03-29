import Multicast.MC;

import java.net.DatagramPacket;

public class Task {

    private Multicast MC;

    public Task(Multicast MC) {
        this.MC = MC;
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

    private Multicast MDB;

    private String channel;

    public Listener(Multicast MC, Multicast MDB, String channel) {
        super(MC);
        this.MDB = MDB;
        this.channel = channel;
    }

    @Override
    public void run() {
        DatagramPacket packet;

        switch(channel) {
            case "MDB":
                while(true) {
                    packet = MDB.receive_packet();

                    Message message = new Message(packet.getData().toString());


                }
            case "MC":
                while (true) {

                }
        }
    }


}
