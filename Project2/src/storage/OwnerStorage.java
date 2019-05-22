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

public class OwnerStorage {
    private static Random secureRandom = new SecureRandom();

    static final Path ownerDir = Peer.rootPath.resolve("owner");

    private static final ConcurrentHashMap<String, OwnerFile> ownerMap = new ConcurrentHashMap<>();

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

    public static void storeOwner(List<String> fileMetadata) throws NoSuchAlgorithmException, IOException {
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

    public static String listFiles() {
        final StringBuilder sb = new StringBuilder();
        ownerMap.forEachValue(1, sb::append);
        return sb.toString();
    }
}
