package Peer;

import Storage.StorageManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {
    private static final ExecutorService backupThreadPool = Executors.newFixedThreadPool(10);
    private static final ExecutorService downloadThreadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        StorageManager.initStorage();
    }
}
