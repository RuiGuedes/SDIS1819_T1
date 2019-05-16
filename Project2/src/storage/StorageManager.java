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
                    case "chunks":
                        chunkStorage = new ChunkStorage(directory);
                        break;
                    default:
                        throw new SecurityException();
                }
            }
        }
        else {
            Files.createDirectory(rootPath);

            ownerStorage = new OwnerStorage();
            chunkStorage = new ChunkStorage();
        }
    }
}
