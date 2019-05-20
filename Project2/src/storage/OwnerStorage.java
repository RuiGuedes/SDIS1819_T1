package storage;

import peer.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Random;

class OwnerStorage extends Storage<OwnerFile> {
    private static Random secureRandom = new SecureRandom();

    static final String dirName = "owner";

    OwnerStorage() throws IOException {
        super(dirName);
    }

    OwnerStorage(Path ownerDirectory) throws IOException {
        super(ownerDirectory);
    }

    @Override
    OwnerFile valueFromFile(Path file) throws IOException {
        return new OwnerFile(file);
    }

    void storeOwner(List<String> fileMetadata) throws NoSuchAlgorithmException, IOException {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);

        final byte[] peerId = Peer.PEER_ID.getBytes(), inputBytes = new byte[salt.length + peerId.length];
        final ByteBuffer inputBuffer = ByteBuffer.allocate(salt.length + peerId.length).put(salt).put(peerId).flip();
        inputBuffer.get(inputBytes);

        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        final String saltString = Base64.getEncoder().encodeToString(salt);
        final String hashString = Base64.getEncoder().encodeToString(sha256.digest(inputBytes));

        this.fileMap.put(fileMetadata.get(0), new OwnerFile(fileMetadata, saltString, hashString));
    }
}
