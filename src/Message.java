import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Message {

    /**
     * Maximum message size
     */
    static Integer MESSAGE_SIZE = 65536;

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
     * File Id
     */
    private String file_id;

    /**
     * Chunk number
     */
    private Integer chunk_no;

    /**
     * Replication degree
     */
    private Integer replication_degree;

    /**
     * Chunk
     */
    private byte[] body;

    /**
     * Message constructor by specifying each needed field
     */
    Message(String message_type, String protocol_version, Integer server_id, String file_id, Integer chunk_no, Integer replication_degree, byte[] body) {
        // Initializes class variables
        this.message_type = message_type;
        this.protocol_version = protocol_version;
        this.server_id = server_id;
        this.file_id = file_id;
        this.chunk_no = chunk_no;
        this.replication_degree = replication_degree;
        this.body = body;

        // Initializes message header
        init_message_header();
    }

    /**
     * Message constructor throughout a byte array
     * @param packet_info Byte array containing header (0) and body (1)
     */
    Message(ArrayList<byte[]> packet_info) {
        String[] fields = clean_array(bytes_to_string(packet_info.get(0)).split(" "));

        this.message_type = fields[0];
        this.protocol_version = fields[1];
        this.server_id = Integer.parseInt(fields[2]);
        this.file_id = fields[3];

        if(!this.message_type.equals("DELETE"))
            this.chunk_no = Integer.parseInt(fields[4]);

        if(this.message_type.equals("PUTCHUNK"))
            this.replication_degree = Integer.parseInt(fields[5]);

        if(this.message_type.equals("PUTCHUNK") || this.message_type.equals("CHUNK")) {
            this.body = packet_info.get(1).length == 0 ? null : Arrays.copyOf(packet_info.get(1), packet_info.get(1).length);
        }

    }

    /**
     * Initializes message header
     */
    private void init_message_header() {
        this.header  =  this.message_type + " " +
                        this.protocol_version + " " +
                        this.server_id + " " +
                        this.file_id;

        this.header += this.chunk_no != null ? (" " + this.chunk_no) : "";
        this.header += this.replication_degree != null ? (" " + this.replication_degree) : "";
        this.header += "\r\n\r\n";
    }

    /**
     * Cleans header of message by removing non-needed spaces
     * @param list Header message fields
     * @return Cleaned header
     */
    private static String[] clean_array(String[] list) {
        List<String> cleaned = new ArrayList<>();

        for (String s: list) {
            if (s.length() > 0)
                cleaned.add(s);
        }
        return cleaned.toArray(new String[0]);
    }

    /**
     * Decrypts packet to byte array
     * @param bytes Received packet
     * @return Return byte array containing header and body
     */
    static ArrayList<byte[]> decrypt_packet(byte[] bytes) {
        ArrayList<byte[]> packet_info = new ArrayList<>();

        for(int i = 0; i < bytes.length - 3; i++) {
            if(bytes[i] == '\r' && bytes[i + 1] == '\n' && bytes[i + 2] == '\r' && bytes[i + 3] == '\n') {
                byte[] header = Arrays.copyOfRange(bytes, 0 , i); packet_info.add(header);
                byte[] body = Arrays.copyOfRange(bytes, i + 4, bytes.length); packet_info.add(body);
            }
        }

        return packet_info;
    }

    /**
     * Given a message, creates a byte array to be sent
     * @return Byte array containing all information
     */
    byte[] get_data() {
        byte[] data = new byte[get_header().getBytes().length + (get_body() != null ? get_body().length : 0)];
        System.arraycopy(get_header().getBytes(), 0, data, 0, get_header().getBytes().length);

        if(get_body() != null)
            System.arraycopy(get_body(), 0, data, get_header().getBytes().length, get_body().length);

        return data;
    }

    /**
     * Transforms byte array to string
     * @param bytes Byte array
     * @return String
     */
    private static String bytes_to_string(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Get's message type
     */
    String get_message_type() {
        return message_type;
    }

    /**
     * Get's protocol version
     */
    String get_protocol_version() {
        return protocol_version;
    }

    /**
     * Get's server id
     */
    Integer get_server_id() {
        return server_id;
    }

    /**
     * Get's file id
     */
    String get_file_id() {
        return file_id;
    }

    /**
     * Get's header
     */
    private String get_header() {
        return header;
    }

    /**
     * Get's chunk number
     */
    Integer get_chunk_no() {
        return chunk_no;
    }

    /**
     * Get's desired replication degree
     */
    Integer get_replication_degree() {
        return replication_degree;
    }

    /**
     * Get's body
     */
    byte[] get_body() {
        return body;
    }

}
