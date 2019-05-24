package middleware;

import chord.CustomInetAddress;
import peer.Chunk;
import storage.ChunkStorage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
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
                final SSLSocket transferSocket = (SSLSocket) listenerSocket.accept();
                requestPool.execute(() -> {
                    try {
                        ByteBuffer chunkBuffer = ByteBuffer.wrap(transferSocket.getInputStream().readAllBytes());
                        ChunkStorage.store(Chunk.generateId(chunkBuffer.flip()), chunkBuffer);
                    } catch (IOException | NoSuchAlgorithmException | ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void transmitChunk(CustomInetAddress targetPeer, String chunkId)
            throws IOException, ExecutionException, InterruptedException {
        final SSLSocket transmitSocket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());

        final ByteBuffer chunkBuffer = ChunkStorage.get(chunkId);

        byte[] chunkBytes = new byte[chunkBuffer.remaining()];
        chunkBuffer.get(chunkBytes);

        transmitSocket.getOutputStream().write(chunkBytes);
    }
}
