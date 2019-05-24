package peer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Responsible for handling chunk specific operations
 */
public class Chunk {
    public static final int CHUNK_SIZE = 264144;

    /**
     * Generates a chunk identifier given a chunk's content.
     *
     * The identifier is a SHA-256 hash using the chunk's content as input
     *
     * @param dataBuffer Buffer containing a chunk's content
     *
     * @return The chunk's identifier
     *
     * @throws NoSuchAlgorithmException on error retrieving the hashing algorithm
     */
    public static String generateId(ByteBuffer dataBuffer) throws NoSuchAlgorithmException {
        final byte[] data = new byte[dataBuffer.remaining()];
        dataBuffer.get(data);
        dataBuffer.rewind();

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return String.format("%064x", new BigInteger(1, sha256.digest(data)));
    }
}
