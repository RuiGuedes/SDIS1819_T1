import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileData {

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
     * Cursor position in the file
     */
    private Integer offset = 0;

    private Boolean EOF = false;

    /**
     * Default constructor
     * @param filepath
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
    }

    public InputStream get_stream() {
        return stream;
    }

    public String get_filename() { return filename; }

    /**
     * Return String with filename, last_modified and owner
     * @return
     */
    public String get_file_id() {
        return encrypt_file(this.filename + this.last_modified + this.owner);
    }

    public File get_file() {
        return file;
    }

    public long get_last_modified() { return last_modified; }

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
     * @return
     */
    public byte[] next_chunk() {

        if (this.EOF) return null;

        byte[] chunk = new byte[0];
        int bytes_read = 0;
        try {
             bytes_read = this.stream.readNBytes(chunk, this.offset, 64000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.offset += bytes_read;

        if (bytes_read < 64000) this.EOF = true;

        return chunk;
    }
}
