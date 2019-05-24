package middleware;

import chord.CustomInetAddress;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for listening to Chunk Transfer request by other Peers and requesting chunks to other peers
 */

// TODO Setup Protocols and Cyphers

public class ChunkTransfer implements Runnable {
    private final static ExecutorService requestPool = Executors.newCachedThreadPool();

    private final static SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private final SSLServerSocket listenerSocket;

    /**
     * Listens to request on the specified port
     * @param port Port to be listening on
     *
     * @throws IOException on error creating the socket
     */
    public ChunkTransfer(int port) throws IOException {
        final SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        listenerSocket = (SSLServerSocket) ssf.createServerSocket(port);
        listenerSocket.setNeedClientAuth(true);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SSLSocket transferSocket = (SSLSocket) listenerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void requestChunk(CustomInetAddress targetPeer, String chunkId) throws IOException {
        final SSLSocket requestSocket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());
    }
}
