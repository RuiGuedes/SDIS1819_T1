package peer;

import middleware.RequestListener;
import storage.StorageManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {
    private static final ExecutorService backupThreadPool = Executors.newFixedThreadPool(10);
    private static final ExecutorService downloadThreadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try {
            StorageManager.initStorage();
            new Thread(new RequestListener(Integer.parseInt(args[0]))).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
