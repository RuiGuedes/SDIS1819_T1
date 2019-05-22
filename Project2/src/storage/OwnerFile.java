package storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Owner File
 *
 * A Owner File contains the backed up file metadata:
 * - File Name
 * - File Length
 * - Chunk Identifiers (Appended one after another)
 *
 * It also contains security information, such as a randomly generated salt and a hash of this salt
 * prepended to the peer's unique identifier.
 */
public class OwnerFile {
    private final String name;
    private final String length;

    /**
     * Creates a owner file from the given information
     *
     * @param fileMetadata Backed up file metadata
     * @param saltString Generated salt in a string format
     * @param hashString Generated hash in a string format
     *
     * @throws IOException on error creating the owner file
     */
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

    /**
     * Imports an existing owner file to be tracked by the system
     *
     * @param ownerFile Path of the owner file
     *
     * @throws IOException on error reading the owner file
     */
    OwnerFile(Path ownerFile) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(ownerFile, StandardCharsets.UTF_8)) {
            this.name = br.readLine();
            this.length = br.readLine();
        }
    }

    /**
     * @return Format of an owner file to be presented on a list of owner files
     */
    @Override
    public String toString() {
        return name + '\t' + length + System.lineSeparator();
    }

    /**
     * Utility function for seperating the appened chunkHashes, retrieved from a owner file
     *
     * @param chunkHashes Chunk identifiers appended to each other
     * @return List containing each chunk identifier seperated
     */
    public static String[] detachChunks(String chunkHashes) {
        final List<String> chunkList = new ArrayList<>();

        for (int i = 0; i < chunkHashes.length(); i+= 64) {
            chunkList.add(chunkHashes.substring(i, i + 64));
        }

        return chunkList.toArray(String[]::new);
    }
}
