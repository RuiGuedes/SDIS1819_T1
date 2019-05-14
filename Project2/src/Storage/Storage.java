package Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

abstract class Storage<T> {
    private final ConcurrentHashMap<String, T> fileMap = new ConcurrentHashMap<>();

    Storage(String dirName) {
        StorageManager.rootPath.resolve(dirName);
    }

    Storage(Path directory) throws IOException {
        for (Path file : Files.newDirectoryStream(directory)) {
            fileMap.put(file.getFileName().toString(), ValueFromFile(file));
        }
    }

    abstract T ValueFromFile(Path file) throws IOException;
}
