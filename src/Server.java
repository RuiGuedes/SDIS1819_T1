import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Server implements RMI {

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
     * Default constructor
     */
    private Server() {}

    /**
     * Initializes the server program to be ready for use
     * @param args Contains all needed variables for setup the server program
     */
    public static void main(String[] args) {
        // Check arguments
        if (args.length != 6) {
            System.out.println("Usage: java Server <protocol version> <server id> <service access point> <MC> <MDB> <MDR>");
            System.out.println("MC  - Multicast channel for control messages | MDB - Multicast channel used for backup | MDR - Multicast channel used for restore");
            return;
        }

        // Initialize class variables
        PROTOCOL_VERSION = args[0];
        STUB_NAME = args[2];
        SERVER_ID = Integer.parseInt(args[1]);
        MULTICAST.put("MC", args[3].split(":", -1)); MULTICAST.put("MDB", args[4].split(":", -1)); MULTICAST.put("MDR", args[5].split(":", -1));

        // Initialize remote object
        init_remote_object();
    }

    /**
     * Creates and exports a remote object and binds it to the name provided (STUB_NAME)
     */
    private static void init_remote_object() {
        try {
            // Create and export a remote object
            Server remote_object = new Server();
            RMI stub = (RMI) UnicastRemoteObject.exportObject(remote_object, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();

            registry.bind(STUB_NAME, stub);
            System.out.println("Server " + SERVER_ID + " ready");
        } catch (Exception e) {
            System.out.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public String backup(String filename, Integer replication_degree) throws RemoteException {



        return "Backup of " + filename + " has been done with success !";
    }

    @Override
    public String restore(String filename) throws RemoteException {
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
