import Multicast.MC;

public class Task {

    private Multicast MC;

    public Task(Multicast MC) {
        this.MC = MC;
    }


}

class PutChunk extends Task implements Runnable {

    private Multicast MC;

    public PutChunk(Multicast MC, Multicast MDB) {
        super(MC);
    }

    @Override
    public void run() {

    }
}
