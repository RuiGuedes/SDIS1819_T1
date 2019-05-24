package middleware;

import chord.CustomInetAddress;
import chord.Query;
import chord.Utilities;
import peer.Chunk;
import storage.ChunkStorage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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

    public static void uploadChunk(String chunkId) throws IOException {
        final List<CustomInetAddress> candidateNodes = Query.findTargetAddress(Long.parseLong(chunkId));

        for (CustomInetAddress target : candidateNodes) {
            if (transmitChunk(target, chunkId)) return;
        }

        throw new IOException();
    }

    public static void deleteChunk(String chunkId) throws IOException {
        final List<CustomInetAddress> targetNodes = Query.findTargetAddress(Long.parseLong(chunkId));

        for (CustomInetAddress target : targetNodes) {
            if (!Utilities.sendRequest(target, "DELETE_CHUNK:" + chunkId).equals("SUCCESS"))
                throw new IOException();
        }
    }

    public static boolean transmitChunk(CustomInetAddress targetPeer, String chunkId) {
        final SSLSocket transmitSocket;
        try {
            transmitSocket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());
            final ByteBuffer chunkBuffer = ChunkStorage.get(chunkId);

            byte[] chunkBytes = new byte[chunkBuffer.remaining()];
            chunkBuffer.get(chunkBytes);

            transmitSocket.getOutputStream().write(chunkBytes);

            return transmitSocket.getInputStream().read() != -1;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }

    }
}
