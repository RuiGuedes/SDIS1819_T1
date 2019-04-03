import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class FileData {

    /**
     * Name of file
     */
    private String filename;

    /**
     * Object file
     */
    private File file;

    /**
     * File stream
     */
    private InputStream stream;

    /**
     * Date of last modification
     */
    private long last_modified;

    /**
     * Owner of the file
     */
    private String owner;

    /**
     * Encrypted fileId using SHA-256
     */
    private String file_id;

    /**
     * Determines end-of-file
     */
    private Boolean EOF = false;

    /**
     * Default constructor
     */
    FileData(String filepath) {
        this.file = new File(filepath);
        this.filename = this.file.getName();
        try {
            this.stream = new FileInputStream(this.file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.last_modified = this.file.lastModified();
        try {
            this.owner = String.valueOf(Files.getOwner(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.file_id = encrypt_file(this.filename + this.last_modified + this.owner);
    }

    /**
     * Get filename
     */
    String get_filename() {
        return filename; }

    /**
     * Return String with filename, last_modified and owner encrypted
     */
    String get_file_id() {
        return file_id;
    }

    /**
     * Encrypts fileId using SHA-256
     * @param file_id FileId
     * @return Encrypted fileId
     */
    private String encrypt_file(String file_id) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(file_id.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts bytes to hexadecimal
     * @param hash Bytes to be converted
     * @return Returns converted bytes
     */
    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (byte element : hash) {
            String hex = Integer.toHexString(0xff & element);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Read and return the next chunk of the file
     * @return Chunk information in byte array
     */
    byte[] next_chunk() {
        if (this.EOF)
            return null;

        byte[] chunk = new byte[64000];
        int bytes_read = 0;
        try {
             bytes_read = this.stream.readNBytes(chunk, 0, 64000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bytes_read < 64000)
            this.EOF = true;

        return Arrays.copyOf(chunk, bytes_read);
    }
}
