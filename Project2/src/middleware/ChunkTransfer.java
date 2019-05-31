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
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
        listenerSocket = (SSLServerSocket) ssf.createServerSocket(port, 50);
        listenerSocket.setNeedClientAuth(true);
        listenerSocket.setEnabledCipherSuites(new String[] {
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        });
    }

    /**
     * Method responsible for accepting requests and processing them
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final SSLSocket transferSocket = (SSLSocket) listenerSocket.accept();
                requestPool.execute(() -> {
                    try {
                        final DataInputStream in = new DataInputStream(transferSocket.getInputStream());
                        final DataOutputStream out = new DataOutputStream(transferSocket.getOutputStream());
                        if (in.read() == TRANSMIT) {
                            final byte[] chunkData = new byte[in.readInt()];
                            in.readFully(chunkData, 0, chunkData.length);

                            final ByteBuffer chunkBuffer = ByteBuffer.wrap(chunkData);
                            try {
                                ChunkStorage.store(Chunk.generateId(chunkBuffer), chunkBuffer);
                                out.write(1);
                            } catch (IOException e) {
                                e.printStackTrace();
                                out.write(0);
                            }
                        }
                        else {
                            final String chunkId = in.readUTF();

                            final ByteBuffer chunkBuffer = ChunkStorage.get(chunkId);
                            final byte[] data = new byte[chunkBuffer.remaining()];
                            chunkBuffer.get(data);

                            out.writeInt(data.length);
                            out.write(data, 0, data.length);
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

    /**
     * Uploads a Chunk to the Network
     *
     * @param chunkId Chunk Identifier
     * @param chunkData Chunk Contents
     *
     * @throws IOException on failure to upload the chunk
     */
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
                } catch (IOException e) {
                    System.out.println("Couldn't store chunk id " + chunkId);
                }
            }
            else {
                if (transmitChunk(target, chunkData)) uploaded = true;
            }
        }

        if (!uploaded) throw new IOException();
    }

    /**
     * Downloads a Chunk from the Network
     *
     * @param chunkId Chunk Identifier
     *
     * @throws IOException on failure to download the chunk
     */
    public static ByteBuffer downloadChunk(String chunkId) throws IOException {
        final List<CustomInetAddress> candidateNodes = Query.findTargetAddress(Utilities.hashCode(chunkId));

        for (CustomInetAddress target : candidateNodes) {
            if (target.isSelf()) {
                try {
                    return ChunkStorage.get(chunkId);
                } catch (IOException | ExecutionException | InterruptedException e) {
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

    /**
     * Deletes a Chunk from the Network
     *
     * @param chunkId Chunk Identifier
     *
     * @throws IOException on failure to delete the chunk
     */
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

    /**
     * Transmits a Chunk to a target peer
     *
     * @param targetPeer Address of the target peer
     * @param chunkData Chunk's contents
     *
     * @return Whether the Transmission was successful or not
     */
    public static boolean transmitChunk(CustomInetAddress targetPeer, ByteBuffer chunkData) {
        final SSLSocket socket;
        try {
            socket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());
            socket.setEnabledCipherSuites(new String[] {
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
            });

            byte[] chunkBytes = new byte[chunkData.remaining()];
            chunkData.get(chunkBytes);
            chunkData.rewind();

            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(TRANSMIT);
            out.writeInt(chunkBytes.length);
            out.write(chunkBytes, 0, chunkBytes.length);

            return socket.getInputStream().read() != 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves a Chunk from a peer
     *
     * @param targetPeer Address of the peer to retrieve the chunk from
     * @param chunkId Chunk Identifier
     *
     * @return Chunk's contents
     */
    private static ByteBuffer receiveChunk(CustomInetAddress targetPeer, String chunkId) {
        final SSLSocket socket;
        try {
            socket = (SSLSocket) sf.createSocket(targetPeer.getAddress(), targetPeer.getPort());
            socket.setEnabledCipherSuites(new String[] {
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
            });

            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(RECEIVE);
            out.writeUTF(chunkId);

            final DataInputStream in = new DataInputStream(socket.getInputStream());
            final byte[] chunkData = new byte[in.readInt()];
            in.readFully(chunkData, 0, chunkData.length);

            return ByteBuffer.wrap(chunkData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
