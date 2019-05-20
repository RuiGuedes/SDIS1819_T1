package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

class OwnerFile {
    private final String name;
    private final String length;
    private final String[] chunkIds;

    private final Path ownerFile;

    OwnerFile(List<String> fileMetadata, String saltString, String hashString) throws IOException {
        this.name = fileMetadata.get(0);
        this.length = fileMetadata.get(1);
        this.chunkIds = detachChunks(fileMetadata.get(2));

        fileMetadata.add(saltString);
        fileMetadata.add(hashString);

        this.ownerFile = Files.write(
                StorageManager.rootPath.resolve(OwnerStorage.dirName)
                        .resolve(hashString.replace('/', '_')),
                fileMetadata,

                StandardCharsets.UTF_8,

                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    OwnerFile(Path ownerFile) throws IOException {
        List<String> fileContents = Files.readAllLines(ownerFile, StandardCharsets.UTF_8);

        this.name = fileContents.remove(0);
        this.length = fileContents.remove(0);
        this.chunkIds = detachChunks(fileContents.remove(0));

        this.ownerFile = ownerFile;
    }

    private String[] detachChunks(String chunkHashes) {
        final List<String> chunkList = new ArrayList<>();

        for (int i = 0; i < chunkHashes.length(); i+= 64) {
            chunkList.add(chunkHashes.substring(i, i + 64));
        }

        return chunkList.toArray(String[]::new);
    }
}
