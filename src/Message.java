import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    /**
     * File Id
     */
    private String file_id;

    /**
     * Chunk number
     */
    private String chunk_no = null;

    /**
     * Replication degree
     */
    private String replication_degree = null;

    /**
     * Chunk
     */
    private String body = null;

    Message(String message_type, String protocol_version, Integer server_id, FileData file) {
        // Initializes class variables
        this.file = file;
        this.server_id = server_id;
        this.message_type = message_type;
        this.protocol_version = protocol_version;
        this.file_id = encrypt_file(file.get_file_id() + file.get_last_modified());

        // Initializes message header
        init_message_header();
    }

    Message(String message_type, String protocol_version, Integer server_id, String file_id, String chunk_no) {
        // Initializes class variables
        this.message_type = message_type;
        this.protocol_version = protocol_version;
        this.server_id = server_id;
        this.file_id = file_id;
        this.chunk_no = chunk_no;

    }

    Message(String protocol) {
        String[] fields = Peer.clean_array(protocol.split(" "));

        this.message_type = fields[0];
        this.protocol_version = fields[1];
        this.server_id = Integer.parseInt(fields[2]);
        this.file_id = fields[3];

        if (this.message_type != "DELETE") {
            this.chunk_no = fields[4];
        }

        if (this.message_type == "PUTCHUNK") {
            this.replication_degree = fields[5];
            // <CRLF><CRLF> = fields[6]
            this.body = fields[7];
        }

        if (this.message_type == "CHUNK")
            this.body = fields[6];

//        PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
//        STORED   <Version> <SenderId> <FileId> <ChunkNo>                  <CRLF><CRLF>
//        GETCHUNK <Version> <SenderId> <FileId> <ChunkNo>                  <CRLF><CRLF>
//        CHUNK    <Version> <SenderId> <FileId> <ChunkNo>                  <CRLF><CRLF><Body>
//        DELETE   <Version> <SenderId> <FileId>                            <CRLF><CRLF>
//        REMOVED  <Version> <SenderId> <FileId> <ChunkNo>                  <CRLF><CRLF>
    }

    /**
     * Initializes message header
     */
    private void init_message_header() {
        this.header  =  this.message_type + " " +
                        this.protocol_version + " " +
                        this.server_id + " " +
                        this.file_id;
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
        return header + convert_to_string(info) + "\r\n\r\n";
    }

    /**
     * Convert an array list to string with header specific format
     * @param info Extra information to be converted to string
     * @return Converted string
     */
    private String convert_to_string(Integer[] info) {
        String information = "";
        for(Integer element : info) {
            information += element + " ";
        }
        return information;
    }

    public String getMessage_type() {
        return message_type;
    }

    public String getProtocol_version() {
        return protocol_version;
    }

    public Integer getServer_id() {
        return server_id;
    }

    public String getFile_id() {
        return file_id;
    }

    public String getHeader() {
        return header;
    }

    public String getChunk_no() {
        return chunk_no;
    }

    public void setChunk_no(Integer chunk_no) {
        this.chunk_no = String.valueOf(chunk_no);
    }

    public String getReplication_degree() {
        return replication_degree;
    }

    public void setReplication_degree(Integer replication_degree) {
        this.replication_degree = String.valueOf(replication_degree);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
