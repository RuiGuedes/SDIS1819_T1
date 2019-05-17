package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

abstract class Storage<T> {
    final ConcurrentHashMap<String, T> fileMap = new ConcurrentHashMap<>();

    Storage(String dirName) throws IOException {
        Files.createDirectory(StorageManager.rootPath.resolve(dirName));
    }

    Storage(Path directory) throws IOException {
        for (Path file : Files.newDirectoryStream(directory)) {
            fileMap.put(file.getFileName().toString(), valueFromFile(file));
        }
    }

    abstract T valueFromFile(Path file) throws IOException;
}
