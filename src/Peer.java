import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


public class Peer implements RMI {

    // SERVER 1 - "vanilla" 1 "rmi-access-point" "224.0.0.2:4441" "224.0.0.3:4442" "224.0.0.4:4443"
    // SERVER 2 - "vanilla" 2 "rmi-access-point2" "224.0.0.2:4441" "224.0.0.3:4442" "224.0.0.4:4443"

    /**
     * Protocol version to be used. Default = vanilla
     */
    private static String PROTOCOL_VERSION;

    /**
     * Service access point to be used by clients
     */
    private static String STUB_NAME;

    /**
     * Service server id
     */
    private static Integer SERVER_ID;

    /**
     * Contains all multicast (MC, MDB, MDR) information in an array following the specific format:
     * String[0] - Multicast Address | String[1] - Multicast Port
     */
    private static Map<String, String[]> MULTICAST = new HashMap<String, String[]>();

    /**
     * Multicast channel used for control messages
     */
    private static Multicast MC;

    /**
     * Multicast channel used to backup files
     */
    private static Multicast MDB;

    /**
     * Multicast channel used to restore files
     */
    private static Multicast MDR;

    /**
     * Storage class
     */
    private static Storage storage;

    /**
     * Default constructor
     */
    private Peer() {}

    /**
     * Initializes the server program to be ready for use
     * @param args Contains all needed variables for setup the server program
     */
    public static void main(String[] args) {
        // Check arguments
        if (args.length != 6) {
            System.out.println("Usage: java Peer <protocol version> <server id> <service access point> <MC> <MDB> <MDR>");
            System.out.println("MC  - Multicast channel for control messages | MDB - Multicast channel used for backup | MDR - Multicast channel used for restore");
            return;
        }

        // Initialize class variables
        PROTOCOL_VERSION = args[0];
        STUB_NAME = args[2];
        SERVER_ID = Integer.parseInt(args[1]);
        MULTICAST.put("MC", args[3].split(":", -1));
        MULTICAST.put("MDB", args[4].split(":", -1));
        MULTICAST.put("MDR", args[5].split(":", -1));

        // Initializes storage
        storage = new Storage(SERVER_ID);

        // Initialize remote object
        init_remote_object();

        // Initializes multicast channels
        init_multicast_channels();
    }

    /**
     * Creates and exports a remote object and binds it to the name provided (STUB_NAME)
     */
    private static void init_remote_object() {
        try {
            // Create and export a remote object
            Peer remote_object = new Peer();
            RMI stub = (RMI) UnicastRemoteObject.exportObject(remote_object, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();

            registry.bind(STUB_NAME, stub);
            System.out.println("Peer " + SERVER_ID + " ready");
        } catch (Exception e) {
            System.out.println("Peer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Initializes multicast channels and starts listening MC & MDB
     */
    private static void init_multicast_channels() {
        // Creates each multicast channel
        MC = new Multicast("MC", MULTICAST.get("MC")[0], MULTICAST.get("MC")[1]);
        MDB = new Multicast("MDB", MULTICAST.get("MDB")[0], MULTICAST.get("MDB")[1]);
        MDR = new Multicast("MDR", MULTICAST.get("MDR")[0], MULTICAST.get("MDR")[1]);

        // Starts listening channels: MC & MDB
        MC.getExecuter().execute(new Listener(MC));
        MDB.getExecuter().execute(new Listener(MDB));
    }

    /**
     * Returns protocol version
     * @return Protocol version
     */
    public static String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }

    /**
     * Returns server id
     * @return Server id
     */
    public static Integer getServerId() {
        return SERVER_ID;
    }

    /**
     * Retrieves MC multicast channel
     * @return Returns MC multicast channel
     */
    public static Multicast getMC() {
        return MC;
    }

    /**
     * Retrieves MDB multicast channel
     * @return Returns MDB multicast channel
     */
    public static Multicast getMDB() {
        return MDB;
    }

    /**
     * Retrieves MDR multicast channel
     * @return Returns MDR multicast channel
     */
    public static Multicast getMDR() {
        return MDR;
    }

    /**
     * Get peer storage structure
     * @return Storage class
     */
    public static Storage getStorage() {
        return storage;
    }

    @Override
    public String backup(String filepath, Integer replication_degree) {
        // Variables
        byte[] chunk;
        int chunk_no = 0;
        FileData file = new FileData(filepath);

        while ((chunk = file.next_chunk()) != null) {
            Message message = new Message("PUTCHUNK", PROTOCOL_VERSION, SERVER_ID, file.get_file_id(), chunk_no++, replication_degree);

            byte[] data = new byte[message.get_header().getBytes().length + chunk.length];
            System.arraycopy(message.get_header().getBytes(), 0, data, 0, message.get_header().getBytes().length);
            System.arraycopy(chunk, 0, data, message.get_header().getBytes().length, chunk.length);

            /*System.out.println("0 - " + chunk.length);
            System.out.println("1 - " + message.get_header().length());
            System.out.println("1.1 - " + message.get_header().getBytes().length);
            System.out.println("2 - " + Message.bytes_to_string(chunk).length());
            System.out.println("2.1 - " + Message.bytes_to_string(chunk).getBytes().length);
            System.out.println(data.length);*/
            DatagramPacket packet = new DatagramPacket(data, data.length, MDB.getGroup(), MDB.getPort());
            MDB.getExecuter().execute(new PutChunk(message, packet));
        }

        return "Backup of " + file.get_filename() + " has been done with success !";
    }


    @Override
    public String restore(String filename) {

        // handle MDR channel when is invoked
        // actions needed to handle MDR

        return null;
    }

    @Override
    public String delete(String filepath) {
        FileData file = new FileData(filepath);

        Message message = new Message("DELETE", PROTOCOL_VERSION, SERVER_ID, file.get_file_id(),null,null);

        MC.send_packet(message);

        return file.get_filename() + " has been deleted with success !";
    }

    @Override
    public String reclaim(Integer disk_space) {

        storage.update_storage_space(disk_space);

        return "Storage reclaim has been done with success !";
    }

    @Override
    public String state() {
        return "";
    }
}
