package storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ChunkStorage extends Storage<AsynchronousFileChannel> {
    private static final ExecutorService chunkIOExecutor = Executors.newCachedThreadPool();

    private static final String dirName = "chunk";

    ChunkStorage() throws IOException {
        super(dirName);
    }

    ChunkStorage(Path chunkDirectory) throws IOException {
        super(chunkDirectory);
    }

    @Override
    AsynchronousFileChannel valueFromFile(Path file) throws IOException {
        return AsynchronousFileChannel.open(file, Set.of(), chunkIOExecutor);
    }

    void storeChunk(String chunkId, ByteBuffer chunkData) throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = StorageManager.rootPath.resolve(dirName).resolve(chunkId);

        if (!Files.isRegularFile(chunkFile)) {
            try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                    chunkFile,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
                    chunkIOExecutor
            )) {
                this.fileMap.put(chunkId, afc);

                // Unblock the chunk storage
                afc.write(chunkData, 0).get();
            }
        }
    }

    ByteBuffer getChunk(String chunkId) throws IOException, ExecutionException, InterruptedException {
        final Path chunkFile = StorageManager.rootPath.resolve(dirName).resolve(chunkId);

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
