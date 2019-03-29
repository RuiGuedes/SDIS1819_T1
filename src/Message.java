import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

class Message {

    /**
     * Message type to be sent
     */
    private String message_type;

    /**
     * Protocol version
     */
    private String protocol_version;

    /**
     * Server id
     */
    private Integer server_id;

    /**
     * Fixed header part
     */
    private String header;

    /**
     * File used to construct message
     */
    private FileData file;

    Message(String message_type, String protocol_version, Integer server_id, FileData file) {
        // Initializes class variables
        this.file = file;
        this.server_id = server_id;
        this.message_type = message_type;
        this.protocol_version = protocol_version;

        // Initializes message header
        init_message_header();
    }

    /**
     * Initializes message header
     */
    private void init_message_header() {
        this.header =   this.message_type + " " +
                        this.protocol_version + " " +
                        this.server_id + " " +
                        encrypt_file(file.get_file_id());
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
     * Get's full header information
     * @param info Extra information to be appended the already constructed header
     * @return Full header
     */
    public String get_full_header(Integer[] info) {
        return header + convert_to_header_string(info) + "\r\n\r\n";
    }

    /**
     * Convert an array list to string with header specific format
     * @param info Extra information to be converted to string
     * @return Converted string
     */
    private String convert_to_header_string(Integer[] info) {
        String information = "";
        for(Integer element : info) {
            information += element + " ";
        }
        return information;
    }


}
