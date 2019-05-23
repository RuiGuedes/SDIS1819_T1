package chord;

public class Stabilizer extends Thread {

    private Node node;

    private boolean online;

    Stabilizer(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {
        while(online) {


        }
    }

    public void terminate() {
        this.online = false;
    }
}
