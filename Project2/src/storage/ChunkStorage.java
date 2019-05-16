package storage;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;

class ChunkStorage extends Storage<AsynchronousFileChannel> {
    ChunkStorage() {
        super("chunk");
    }

    ChunkStorage(Path chunkDirectory) throws IOException {
        super(chunkDirectory);
    }

    @Override
    AsynchronousFileChannel ValueFromFile(Path file) throws IOException {
        return AsynchronousFileChannel.open(file);
    }
}
