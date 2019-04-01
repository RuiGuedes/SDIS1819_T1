import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

class Message {

    public static Integer MESSAGE_SIZE = 65535;

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
    private String body;


    Message(String message_type, String protocol_version, Integer server_id, String file_id, Integer chunk_no, Integer replication_degree) {
        // Initializes class variables
        this.message_type = message_type;
        this.protocol_version = protocol_version;
        this.server_id = server_id;
        this.file_id = file_id;
        this.chunk_no = chunk_no;
        this.replication_degree = replication_degree;

        // Initializes message header
        init_message_header();
    }

    Message(String protocol) {
        String header = protocol.substring(0, protocol.indexOf("\r\n\r\n"));

        String[] fields = clean_array(header.split(" "));

        System.out.println(header);
        System.out.println(body);

        this.message_type = fields[0];
        this.protocol_version = fields[1];
        this.server_id = Integer.parseInt(fields[2]);
        this.file_id = fields[3];

        if(!this.message_type.equals("DELETE"))
            this.chunk_no = Integer.parseInt(fields[4]);

        if(this.message_type.equals("PUTCHUNK"))
            this.replication_degree = Integer.parseInt(fields[5]);

        if(this.message_type.equals("PUTCHUNK") || this.message_type.equals("CHUNK"))
            this.body = protocol.substring(protocol.indexOf("\r\n\r\n") + "\r\n\r\n".length());
    }


    static String[] clean_array(String[] list) {
        List<String> cleaned = new ArrayList<>();

        for (String s: list) {
            if (s.length() > 0){
                cleaned.add(s);
            }

        }

        return cleaned.toArray(new String[0]);
    }

    static String bytes_to_string(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
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

    public String get_header() {
        return header;
    }

    public Integer getChunk_no() {
        return chunk_no;
    }

    public Integer getReplication_degree() {
        return replication_degree;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
