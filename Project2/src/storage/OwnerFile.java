package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class OwnerFile {
    private final String name;
    private final long length;
    private final String[] chunkIds;

    private final Path ownerFile;

    OwnerFile(Path backupFile, String[] chunkIds) throws IOException {
        this.name = backupFile.getFileName().toString();
        this.length = Files.size(backupFile);
        this.chunkIds = chunkIds;

        this.ownerFile = Files.write(
                backupFile,

                Arrays.asList(
                        name,
                        String.valueOf(length),
                        String.join("", chunkIds)
                        // TODO Salt and Hash
                ),
                StandardCharsets.UTF_8,

                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    OwnerFile(Path ownerFile) throws IOException {
        List<String> fileContents = Files.readAllLines(ownerFile, StandardCharsets.UTF_8);

        this.name = fileContents.remove(0);
        this.length = Long.parseLong(fileContents.remove(0));

        final String chunkHashes = fileContents.remove(0);

        final List<String> chunkList = new ArrayList<>();
        for (int i = 0; i < chunkHashes.length(); i+= 64) {
            chunkList.add(chunkHashes.substring(i, i + 64));
        }

        this.chunkIds = chunkList.toArray(String[]::new);

        this.ownerFile = ownerFile;
    }
}
