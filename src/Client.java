import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    /**
     * Service access point
     */
    private static String STUB_NAME;

    private static String OPERATION;
    private static String OPND_1;
    private static String OPND_2;

    /**
     * Default Constructor
     */
    private Client() {}

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        // Check arguments
        if (args.length < 2) {
            System.out.println("Usage: java Client <peer_ap> <operation> <opnd_1> [<opnd_2>]");
            return;
        }

        // Initialize class variables
        STUB_NAME = args[0];
        OPERATION = args[1];
        OPND_1 = args.length > 2 ? args[2] : null;
        OPND_2 = args.length > 3 ? args[3] : null;


        //Initialize operation
        init_operation();
    }

    /**
     *
     */
    private static void init_operation() {
        try {
            Registry registry = LocateRegistry.getRegistry(null);
            RMI stub = (RMI) registry.lookup(STUB_NAME);

            String response = null;

            switch(OPERATION) {
                case "BACKUP":
                    response = stub.backup();
                    break;
                case "RESTORE":
                    response = stub.restore();
                    break;
                case "DELETE":
                    response = stub.delete();
                    break;
                case "RECLAIM":
                    response = stub.reclaim();
                    break;
                case "STATE":
                    response = stub.state();
                    break;
                default:
                    response = "BAD OPERATION";
                    break;
            }

            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
