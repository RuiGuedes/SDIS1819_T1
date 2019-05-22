package storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class OwnerFile {
    private final String name;
    private final String length;

    OwnerFile(List<String> fileMetadata, String saltString, String hashString) throws IOException {
        this.name = fileMetadata.get(0);
        this.length = fileMetadata.get(1);

        fileMetadata.add(saltString);
        fileMetadata.add(hashString);

        Files.write(
                OwnerStorage.ownerDir.resolve(hashString.replace('/', '_') + ".own"),
                fileMetadata,

                StandardCharsets.UTF_8,

                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    OwnerFile(Path ownerFile) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(ownerFile, StandardCharsets.UTF_8)) {
            this.name = br.readLine();
            this.length = br.readLine();
        }
    }

    @Override
    public String toString() {
        return name + '\t' + length + System.lineSeparator();
    }

    public static String[] detachChunks(String chunkHashes) {
        final List<String> chunkList = new ArrayList<>();

        for (int i = 0; i < chunkHashes.length(); i+= 64) {
            chunkList.add(chunkHashes.substring(i, i + 64));
        }

        return chunkList.toArray(String[]::new);
    }
}
