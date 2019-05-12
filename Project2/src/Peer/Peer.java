package Peer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {
    private static final ExecutorService backupThreadPool = Executors.newFixedThreadPool(10);
    private static final ExecutorService downloadThreadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("Ho ho ho ha ha, ho ho ho he ha. Hello there old chum. I’m gnot an gnelf. "
                + "I’m gnot a gnoblin. I’m a gnome. And you’ve been, GNOMED!");
    }
}
