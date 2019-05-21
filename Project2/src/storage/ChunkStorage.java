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

public class ChunkStorage {
    private static final ExecutorService chunkIOExecutor = Executors.newCachedThreadPool();

    private static final Path chunkDir = Peer.rootPath.resolve("chunk");

    private static final Set<String> chunkSet = ConcurrentHashMap.newKeySet();

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
}
