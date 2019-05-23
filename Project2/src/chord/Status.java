package chord;

public class Status extends Thread {

    private Node node;

    private boolean online;

    Status(Node node) {
        this.node = node;
        this.online = true;
    }

    @Override
    public void run() {
        while(online) {

            for(int i = 1; i <= Chord.M; i++) {
                System.out.println(i + " -> " + this.node.getIthFinger(i));
            }

            System.out.println("------------------------------------------------------------");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void terminate() {
        this.online = false;
    }
}
