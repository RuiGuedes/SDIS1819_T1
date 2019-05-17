package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class StorageManager {
    // Assuming there's a single peer running per execution environment
    static final Path rootPath = Paths.get("./peer");

    private static OwnerStorage ownerStorage;
    private static ChunkStorage chunkStorage;

    public static void initStorage() throws IOException {
        if (Files.isDirectory(rootPath)) {
            for (Path directory : Files.newDirectoryStream(rootPath)) {
                switch (directory.getFileName().toString()) {
                    case "owner":
                        ownerStorage = new OwnerStorage(directory);
                        break;
                    case "chunk":
                        chunkStorage = new ChunkStorage(directory);
                        break;
                    default:
                        throw new SecurityException("Invalid folder: " + directory.getFileName());
                }
            }
        }
        else {
            Files.createDirectory(rootPath);
        }

        if (ownerStorage == null)   ownerStorage = new OwnerStorage();
        if (chunkStorage == null)   chunkStorage = new ChunkStorage();
    }

    public static OwnerStorage getOwnerStorage() {
        return ownerStorage;
    }

    public static ChunkStorage getChunkStorage() {
        return chunkStorage;
    }
}
