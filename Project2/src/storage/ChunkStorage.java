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

    private static final Set<String> chunkSet = ConcurrentHashMap.newKeySet();

    /**
     * Initializes the chunk storage, restoring from a previous state if it exists
     *
     * @throws IOException on error reading chunk files
     */
    public static void init() throws IOException {
        if (Files.isDirectory(chunkDir)) {
            for (Path file : Files.newDirectoryStream(chunkDir)) {
                chunkSet.add(file.getFileName().toString());
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
     *
     * @throws ExecutionException on error executing the write operation
     * @throws InterruptedException on interruption of the write operation
     */
    public static void storeChunk(String chunkId, ByteBuffer chunkData) throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = chunkDir.resolve(chunkId);

        if (!Files.isRegularFile(chunkFile)) {
            try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                    chunkFile,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
                    chunkIOExecutor
            )) {
                chunkSet.add(chunkId);

                // Unblock the chunk storage
                afc.write(chunkData, 0).get();
            }
        }
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
    public static ByteBuffer getChunk(String chunkId) throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = chunkDir.resolve(chunkId);

        if (!Files.isRegularFile(chunkFile))
            throw new FileNotFoundException();

        try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                chunkFile,
                Set.of(StandardOpenOption.READ),
                chunkIOExecutor
        )) {
            final ByteBuffer chunkData = ByteBuffer.allocate(FileManager.Chunk.CHUNK_SIZE);

            // Unblock the chunk retrieval
            afc.read(chunkData, 0).get();

            return chunkData.flip();
        }
    }

    /**
     * Lists the chunks stored by the peer
     *
     * @return List of a peer's stored chunks, each line containing its identifer
     */
    public static String listFiles() {
        final StringBuilder sb = new StringBuilder();
        chunkSet.forEach((id) -> sb.append(id).append(System.lineSeparator()));
        return sb.toString();
    }
}
