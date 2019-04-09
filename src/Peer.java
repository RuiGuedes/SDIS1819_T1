import java.io.File;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


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
    private static Map<String, String[]> MULTICAST = new HashMap<>();

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

    volatile static Map<String, Boolean> file_backup_status;

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
        file_backup_status = new HashMap<>();

        // Initialize remote object
        init_remote_object();

        // Initializes multicast channels
        init_multicast_channels();

        // Checks if there are files to be removed
        if(PROTOCOL_VERSION.equals("2.0") && Storage.last_execution_date != 0) {
            // Reuse of previous protocol message to send date instead chunk number and ignoring fileID because its not needed for this case
            MC.send_packet(new Message("DELETEDFILES", PROTOCOL_VERSION, SERVER_ID, null, (int) Storage.last_execution_date, null, null));
        }

        // Before terminating, save last execution date in "delete_files" file
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Storage.add_deleted_file("\n")));
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

        // Starts listening channels
        MC.getExecuter().execute(new Listener(MC));
        MDB.getExecuter().execute(new Listener(MDB));
        MDR.getExecuter().execute(new Listener(MDR));
    }

    /**
     * Returns protocol version
     * @return Protocol version
     */
    static String get_protocol_version() {
        return PROTOCOL_VERSION;
    }

    /**
     * Returns server id
     * @return Server id
     */
    static Integer get_server_id() {
        return SERVER_ID;
    }

    /**
     * Retrieves MC multicast channel
     * @return Returns MC multicast channel
     */
    static Multicast getMC() {
        return MC;
    }

    /**
     * Retrieves MDB multicast channel
     * @return Returns MDB multicast channel
     */
    static Multicast getMDB() {
        return MDB;
    }

    /**
     * Retrieves MDR multicast channel
     * @return Returns MDR multicast channel
     */
    static Multicast getMDR() {
        return MDR;
    }

    /**
     * Get peer storage structure
     * @return Storage class
     */
    static Storage getStorage() {
        return storage;
    }

    @Override
    public String backup(String filepath, Integer replication_degree) {
        // Create file data
        FileData file = new FileData(filepath);

        // Variables
        byte[] chunk;
        int chunk_no = 0;
        Set<Callable<Boolean>> threads = new HashSet<>();

        // Determine if file was already backed up or updated
        switch (getStorage().is_backed_up(file.get_filename(), file.get_file_id())) {
            case "RETURN":
                return "Backup of " + file.get_filename() + " has already been done !";
            case "DELETE-AND-BACKUP":
                this.delete(file.get_filename(), getStorage().get_backed_up_file_id(file.get_filename()));
                break;
        }

        // Adds file to backed up files list
        getStorage().store_backed_up_file(file);
        synchronized (file_backup_status) {
            file_backup_status.put(file.get_file_id(), true);
        }

        // Adds file to structure that contains chunks information
        Synchronized.chunks_info_struct.put(file.get_file_id(), new HashMap<>());

        while ((chunk = file.next_chunk()) != null) {
            // Creates message to be sent with the needed variables
            Message message = new Message("PUTCHUNK", PROTOCOL_VERSION, SERVER_ID, file.get_file_id(), chunk_no++, replication_degree, chunk);

            // Transforms message into
            byte[] data = message.get_data();

            // Creates packet to be sent and task to be executed
            DatagramPacket packet = new DatagramPacket(data, data.length, MDB.getGroup(), MDB.getPort());
            PutChunk task = new PutChunk(message, packet);

            // Adds task to threads list
            threads.add(task);
        }

        // Invoke all tasks and check their status
        try {
            MDB.getExecuter().invokeAll(threads);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get backup status
        boolean backup_status;
        synchronized (file_backup_status) {
            backup_status = file_backup_status.get(file.get_file_id());
        }

        if(backup_status)
            Storage.store_chunks_info_of_file(file.get_file_id(), replication_degree);
        else
            this.delete(file.get_filename(), file.get_file_id());

        return "Backup of " + file.get_filename() + " has been done " + (backup_status ? "with" : "without") + " success !";
    }

    @Override
    public String restore(String filepath) {
        // Get filename and its file id
        String filename = new File(filepath).getName();
        String file_id = getStorage().get_backed_up_file_id(filename);

        // Checks if file was previously backed up
        if(file_id.equals(""))
            return "Restore of " + filename + " could not be done since it was not previously backed up !";

        // Variables
        int chunk_no = 0;
        boolean restore_status = false;
        int num_chunks = Storage.get_num_chunks(file_id);

        // Restore protocol enhancement
        if(Peer.get_protocol_version().equals("2.0"))
            Peer.getMDR().getExecuter().execute(new ServerSocketThread(file_id, num_chunks));

        if(Synchronized.synchronized_contains_null_value(file_id))
            System.out.println("TENHO NULLS");

        while (chunk_no < num_chunks) {
            // Creates message to be sent with the needed variables
            Message message = new Message("GETCHUNK", PROTOCOL_VERSION, SERVER_ID, file_id, chunk_no++, null, null);

            // Creates packet to be sent and task to be executed
            MC.send_packet(message);
        }

        // Minor improvement to make restore more robust on possible failures
        for(int i = 0; i < 5; i++) {
            try {
                TimeUnit.SECONDS.sleep((long) Math.pow(2,i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(Synchronized.synchronized_contains_files_to_restore(file_id, null)) {
                if(Synchronized.synchronized_size_files_to_restore(file_id) == num_chunks && !Synchronized.synchronized_contains_null_value(file_id)) {
                    System.out.println("ENTROYU");
                    restore_status = true;
                    break;
                }
                else {
                    for(int j = 0; j < num_chunks; j++) {
                        if(!Synchronized.synchronized_contains_files_to_restore(file_id, j)) {
                            MC.send_packet(new Message("GETCHUNK", PROTOCOL_VERSION, SERVER_ID, file_id, j, null, null));
                        }
                    }
                }
            }
        }

        // Save file on restored files
        if(restore_status)
            getStorage().restore_file(filename, file_id);
        else
            Synchronized.synchronized_remove_files_to_restore(file_id);

        return "Restore of " + filename + " has been done " + (restore_status ? "with" : "without") + " success !";
    }

    @Override
    public String delete(String filepath) {
        FileData file = new FileData(filepath);

        return this.delete(file.get_filename(), file.get_file_id());
    }

    @Override
    public String reclaim(Integer disk_space) {
        storage.set_storage_space(disk_space);
        return "Storage reclaim has been done with success !";
    }

    @Override
    public String state() {
        return getStorage().state();
    }

    /**
     * For a given filename and file_id deletes all evidence of file corresponding to these parameters
     * @param filename Filename
     * @param file_id File id
     */
    private String delete(String filename, String file_id) {
        if(!getStorage().is_backed_up(filename, file_id).equals("RETURN"))
            return "File " + filename + " could not be delete since it was no previously backed up !";

        Message message = new Message("DELETE", PROTOCOL_VERSION, SERVER_ID, file_id, null, null, null);

        getStorage().remove_backed_up_file(filename);
        Storage.delete_file(file_id);

        // Sends delete message three times to prevent its loss
        for(int i = 0; i < 3; i++) {
            try {
                MC.send_packet(message);
                TimeUnit.MILLISECONDS.sleep(new Random().nextInt(400));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Delete protocol enhancement
        if(PROTOCOL_VERSION.equals("2.0"))
            Storage.add_deleted_file(file_id);

        return "Delete of " + filename + " has been done with success !";
    }
}
