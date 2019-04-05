import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    /**
     * Operation to be executed: BACKUP, RESTORE, DELETE, RECLAIM, STATE
     */
    private static String OPERATION;

    /**
     * First operator: filename or disk_space depending on the operation type
     */
    private static String OPND_1;

    /**
     * Second operator: replication degree
     */
    private static Integer OPND_2;

    /**
     * RMI object used to communicate with peers
     */
    private static RMI STUB;

    /**
     * Default Constructor
     */
    private TestApp() {}

    /**
     * Initializes the client program to execute a certain operation
     * @param args Contains all needed variables for setup the client program
     */
    public static <string> void main(String[] args) throws RemoteException, NotBoundException {
        // Check arguments
        if (args.length < 2) {
            System.out.println("Usage: java TestApp <peer_ap> <operation> <opnd_1> [<opnd_2>]");
            return;
        }

        // Initialize class variables
        String STUB_NAME = args[0];
        OPERATION = args[1];
        OPND_1 = args.length > 2 ? args[2] : null;
        OPND_2 = args.length > 3 ? Integer.parseInt(args[3]) : null;

        // Check arguments validity
        if((OPERATION.equals("BACKUP") & OPND_2 == null) || (!OPERATION.equals("STATE") && OPND_1 == null)) {
            System.out.println("Arguments passed incorrectly !");
            return;
        }

        // Get registry in order to find remote object to establish communication
        Registry registry = LocateRegistry.getRegistry(null);
        STUB = (RMI) registry.lookup(STUB_NAME);

       //Initialize operation
        init_operation();
    }

    /**
     * Initialize operation having into account the information passed by as parameter
     */
    private static void init_operation() {
        try {
            switch(OPERATION) {
                case "BACKUP":
                    System.out.println(STUB.backup(OPND_1, OPND_2));
                    break;
                case "RESTORE":
                    System.out.println(STUB.restore(OPND_1));
                    break;
                case "DELETE":
                    System.out.println(STUB.delete(OPND_1));
                    break;
                case "RECLAIM":
                    STUB.reclaim(Integer.parseInt(OPND_1));
                    break;
                case "STATE":
                    System.out.println(STUB.state());
                    break;
                default:
                    System.out.println("Such operation is not available: " + OPERATION + " !");
            }
        } catch (Exception e) {
            System.err.println("TestApp exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
