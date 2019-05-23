package storage;

import peer.FileManager;
import peer.Peer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for managing the storage of chunks
 */
public class ChunkStorage {
    private static final ExecutorService chunkIOExecutor = Executors.newCachedThreadPool();

    private static final Path chunkDir = Peer.rootPath.resolve("chunk");

    private static final ConcurrentHashMap<String, Integer> chunkMap = new ConcurrentHashMap<>();

    /**
     * Initializes the chunk storage, restoring from a previous state if it exists
     *
     * @throws IOException on error reading chunk files
     */
    public static void init() throws IOException {
        if (Files.isDirectory(chunkDir)) {
            for (Path file : Files.newDirectoryStream(chunkDir)) {
                final byte[] intBytes = new byte[4];
                final byte[] inputBytes = (byte[]) Files.getAttribute(file, "user:dependency");
                System.arraycopy(inputBytes, 0, intBytes, 4 - inputBytes.length, inputBytes.length);

                chunkMap.put(file.getFileName().toString(), ByteBuffer.wrap(intBytes).rewind().getInt());
            }
        }
        else {
            Files.createDirectories(chunkDir);
        }
    }

    /**
     * Stores a chunk on the peer's filesystem
     *
     * @param chunkId Identifier of the chunk
     * @param chunkData Contents of the chunk
     *
     * @throws IOException on error creating the chunk file
     * @throws ExecutionException on error executing the write operation
     * @throws InterruptedException on interruption of the write operation
     */
    public static void store(String chunkId, ByteBuffer chunkData)
            throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = chunkDir.resolve(chunkId);

        if (!Files.isRegularFile(chunkFile)) {
            try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                    chunkFile,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
                    chunkIOExecutor
            )) {
                // TODO Unblock the chunk storage
                afc.write(chunkData, 0).get();
            }
        }

        final int dependencyNum = chunkMap.compute(chunkId, (id, value) -> value == null ? 1 : value + 1);
        Files.setAttribute(chunkFile, "user:dependency", ByteBuffer.allocate(4).putInt(dependencyNum).flip());
    }

    /**
     * Retrieves a chunk's content from the peer's filesystem
     *
     * @param chunkId Identifier of the chunk
     *
     * @return Contents of the chunks
     *
     * @throws IOException on error reading the chunk file
     * @throws ExecutionException on error executing the read operation
     * @throws InterruptedException on interruption of the read operation
     */
    public static ByteBuffer get(String chunkId) throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = chunkDir.resolve(chunkId);

        if (!Files.isRegularFile(chunkFile))
            throw new FileNotFoundException();

        try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                chunkFile,
                Set.of(StandardOpenOption.READ),
                chunkIOExecutor
        )) {
            final ByteBuffer chunkData = ByteBuffer.allocate(FileManager.Chunk.CHUNK_SIZE);

            // TODO Unblock the chunk retrieval
            afc.read(chunkData, 0).get();

            return chunkData.flip();
        }
    }

    public static void delete(String chunkId) throws IOException {
        final Path chunkFile = chunkDir.resolve(chunkId);

        final Integer dependencyNum = chunkMap.compute(chunkId, (id, value) -> value == 1 ? null : value - 1);

        if (dependencyNum == null)
            Files.delete(chunkFile);
        else
            Files.setAttribute(chunkFile, "user:dependency", ByteBuffer.allocate(4).putInt(dependencyNum).flip());
    }

    /**
     * Lists the chunks stored by the peer
     *
     * @return List of a peer's stored chunks, each line containing its identifer
     */
    public static String listFiles() throws IOException {
        final StringBuilder sb = new StringBuilder();
        long storageSize = 0L;


        for (String id : chunkMap.keySet()) {
            final long chunkSize = Files.size(chunkDir.resolve(id));

            sb.append(id).append('\t').append(chunkSize).append(System.lineSeparator());
            storageSize += chunkSize;
        }

        return sb.append("Total Storage Size: ").append(storageSize).append(System.lineSeparator()).toString();
    }
}
