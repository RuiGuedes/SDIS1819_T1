package storage;

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
            final AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                    chunkFile,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
                    chunkIOExecutor
            );

            this.fileMap.put(chunkId, afc);

            // When another peer is performing the store, it makes better sense to not block the write
            afc.write(chunkData, 0).get();
            afc.close();
        }
    }
}
