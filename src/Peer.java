import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


public class Peer implements RMI {

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
     * Initializes multicast channels
     */
    private static void init_multicast_channels() {
        // Creates each multicast channel
        MC = new Multicast(MULTICAST.get("MC")[0], MULTICAST.get("MC")[1]);
        MDB = new Multicast(MULTICAST.get("MDB")[0], MULTICAST.get("MDB")[1]);
        MDR = new Multicast(MULTICAST.get("MDR")[0], MULTICAST.get("MDR")[1]);


    }

    @Override
    public String backup(String filepath, Integer replication_degree) {
        // Variables
        byte[] chunk;
        int chunk_no = 0;
        FileData file = new FileData(filepath);

        while ((chunk = file.next_chunk()) != null) {

            Message message = new Message("PUTCHUNK", PROTOCOL_VERSION, SERVER_ID, file.get_file_id(), chunk_no++, replication_degree);

            byte[] data = (message.get_header() + Arrays.toString(chunk)).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, MDB.getGroup(), MDB.getPort());

            PutChunk task = new PutChunk(MC, MDB, message, packet);
            MDB.getExecuter().execute(task);
        }

        return "Backup of " + file.get_filename() + " has been done with success !";
    }


    @Override
    public String restore(String filename) throws RemoteException {

        // handle MDR channel when is invoked
        // actions needed to handle MDR

        return null;
    }

    @Override
    public String delete(String filename) throws RemoteException {
        return null;
    }

    @Override
    public String reclaim(Integer disk_space) throws RemoteException {
        return null;
    }

    @Override
    public String state() throws RemoteException {
        return null;
    }
}
