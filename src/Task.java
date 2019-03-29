import Multicast.MC;

public class Task {

    private Multicast MC;

    public Task(Multicast MC) {
        this.MC = MC;
    }


}

public class PutChunk extends Task implements Runnable {

    private Multicast MC;

    public PutChunk(Multicast MC, Multicast MDB) {
        super(MC);
    }

    @Override
    public void run() {
        try {

        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
