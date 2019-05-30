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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for listening to Chunk Transfer request by other Peers and requesting chunks to other peers
 */

// TODO Setup Protocols and Cyphers

public class ChunkTransfer implements Runnable {
    private static int TRANSMIT = 0;
    private static int RECEIVE = 1;

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
                        final DataInputStream in = new DataInputStream(transferSocket.getInputStream());
                        final OutputStream out = transferSocket.getOutputStream();
                        if (in.read() == TRANSMIT) {
                            final byte[] chunkData = new byte[in.readInt()];
                            in.readFully(chunkData, 0, chunkData.length);

                            final ByteBuffer chunkBuffer = ByteBuffer.wrap(chunkData);
                            ChunkStorage.store(Chunk.generateId(chunkBuffer), chunkBuffer);
                        }
                        else {
                            final String chunkId = new BufferedReader(
                                    new InputStreamReader(in)).readLine();

                            final ByteBuffer chunkBuffer = ChunkStorage.get(chunkId);
                            final byte[] data = new byte[chunkBuffer.remaining()];
                            chunkBuffer.get(data);

                            out.write(data);
                        }

                        transferSocket.close();
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

    public static void uploadChunk(String chunkId, ByteBuffer chunkData) throws IOException {
        boolean uploaded = false;
        final List<CustomInetAddress> candidateNodes = Query.findTargetAddress(Utilities.hashCode(chunkId));

        for (CustomInetAddress target : candidateNodes) {
            if (target.isSelf()) {
                try {
                    ChunkStorage.store(chunkId, chunkData);
                    uploaded = true;
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                if (transmitChunk(target, chunkData)) uploaded = true;
            }
        }

        if (!uploaded) throw new IOException();
    }

    public static ByteBuffer downloadChunk(String chunkId) throws IOException {
        final List<CustomInetAddress> candidateNodes = Query.findTargetAddress(Utilities.hashCode(chunkId));

        for (CustomInetAddress target : candidateNodes) {
            if (target.isSelf()) {
                try {
                    return ChunkStorage.get(chunkId);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                final ByteBuffer data = receiveChunk(target, chunkId);

                if (data != null)   return data;
            }
        }

        throw new IOException();
    }

    public static void deleteChunk(String chunkId) throws IOException {
        final List<CustomInetAddress> targetNodes = Query.findTargetAddress(Utilities.hashCode(chunkId));

        for (CustomInetAddress target : targetNodes) {
            if (target.isSelf()) {
                ChunkStorage.delete(chunkId);
            }
            else {
                if (!Utilities.sendRequest(target, "DELETE_CHUNK:" + chunkId).equals("SUCCESS"))
                    throw new IOException();
            }
        }
    }

    public static boolean transmitChunk(CustomInetAddress targetPeer, ByteBuffer chunkData) {
        final SSLSocket transmitSocket;
        try {
            transmitSocket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());

            byte[] chunkBytes = new byte[chunkData.remaining()];
            chunkData.get(chunkBytes);
            chunkData.rewind();

            final DataOutputStream out = new DataOutputStream(transmitSocket.getOutputStream());
            out.write(TRANSMIT);
            out.writeInt(chunkBytes.length);
            out.write(chunkBytes, 0, chunkBytes.length);

            return transmitSocket.getInputStream().read() != 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static ByteBuffer receiveChunk(CustomInetAddress targetPeer, String chunkId) {
        final SSLSocket receiveSocket;
        try {
            receiveSocket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());

            final PrintWriter out = new PrintWriter(receiveSocket.getOutputStream());
            out.write(RECEIVE);
            out.println(chunkId);

            return ByteBuffer.wrap(receiveSocket.getInputStream().readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
