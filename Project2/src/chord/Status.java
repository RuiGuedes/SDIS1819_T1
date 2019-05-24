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
                if(this.node.getIthFinger(i) == null)
                    System.out.println(i + " -> " + this.node.getIthFinger(i));
                else
                    System.out.println(i + " -> " + Utilities.hashCode(this.node.getIthFinger(i).getHostAddress(), this.node.getIthFinger(i).getPort()));
            }

            if(this.node.getPredecessor() == null)
                System.out.println("9 -> " + this.node.getPredecessor());
            else
                System.out.println("9 -> " + Utilities.hashCode(this.node.getPredecessor().getHostAddress(), this.node.getPredecessor().getPort()));

            System.out.println("------------------------------------------------------------");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void terminate() {
        this.online = false;
    }
}
