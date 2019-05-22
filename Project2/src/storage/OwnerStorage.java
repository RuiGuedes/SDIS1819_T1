package storage;

import peer.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for managing the storage of owner files
 *
 * @see OwnerFile
 */
public class OwnerStorage {
    private static Random secureRandom = new SecureRandom();

    static final Path ownerDir = Peer.rootPath.resolve("owner");

    private static final ConcurrentHashMap<String, OwnerFile> ownerMap = new ConcurrentHashMap<>();

    /**
     * Initializes the owner storage, restoring from a previous state if it exists
     *
     * @throws IOException on error reading chunk files
     */
    public static void init() throws IOException {
        if (Files.isDirectory(ownerDir)) {
            for (Path file : Files.newDirectoryStream(ownerDir)) {
                ownerMap.put(file.getFileName().toString(), new OwnerFile(file));
            }
        }
        else {
            Files.createDirectories(ownerDir);
        }
    }

    /**
     * Stores a owner file on the peer's filesystem
     *
     * @param fileMetadata List containing metadata to store on the owner file (File Name, File Length and Chunk Ids)
     *
     * @throws NoSuchAlgorithmException on error retrieving the hashing algorithm
     * @throws IOException on error creating the owner file
     */
    public static void store(List<String> fileMetadata) throws NoSuchAlgorithmException, IOException {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);

        final byte[] peerId = Peer.PEER_ID.getBytes(), inputBytes = new byte[salt.length + peerId.length];
        final ByteBuffer inputBuffer = ByteBuffer.allocate(salt.length + peerId.length).put(salt).put(peerId).flip();
        inputBuffer.get(inputBytes);

        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        final String saltString = Base64.getEncoder().encodeToString(salt);
        final String hashString = Base64.getEncoder().encodeToString(sha256.digest(inputBytes));

        ownerMap.put(fileMetadata.get(0), new OwnerFile(fileMetadata, saltString, hashString));
    }

    /**
     * Lists the owner files stored by the peer
     *
     * @return List of a peer's stored owner files, each line containing the backed up File Name and Length
     */
    public static String listFiles() {
        final StringBuilder sb = new StringBuilder();
        ownerMap.forEachValue(1, sb::append);
        return sb.toString();
    }
}
