import Multicast.MC;
import Multicast.MDB;
import Multicast.MDR;

import java.io.*;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    public String backup(String filepath, Integer replication_degree) throws IOException {

        FileData file = new FileData(filepath);
        Message message = new Message("PUTCHUNK", PROTOCOL_VERSION, SERVER_ID, file);

        // Socket creation
        InetAddress mdb_group = InetAddress.getByName(MULTICAST.get("MDB")[0]);
        int mdb_port = Integer.parseInt(MULTICAST.get("MDB")[1]);
        MulticastSocket mdb_socket = create_socket(mdb_port, mdb_group);

        InetAddress mc_group = InetAddress.getByName(MULTICAST.get("MC")[0]);
        int mc_port = Integer.parseInt(MULTICAST.get("MC")[1]);
        MulticastSocket mc_socket = create_socket(mc_port, mc_group);

        byte[] chunk = new byte[64000];
        int bytes_readed;
        int chunk_no = 0;



        while((bytes_readed = file.getStream().readNBytes(chunk,chunk_no*64000, 64000)) >= 0) {
            
            String full_header = message.get_full_header(new Integer[]{chunk_no, replication_degree});


            byte[] messagem = (full_header + chunk.toString()).getBytes();
            DatagramPacket packet = new DatagramPacket(messagem,messagem.length, mdb_group, mdb_port);

            PutChunk task = new PutChunk(MC, MDB);

            MDB.getExecuter().execute(task);

            //send_putchunk(replication_degree, header_end, sha256hex, mdb_socket, mc_socket, chunk_no, packet);

            chunk_no++;

            if (bytes_readed < 64000) {
                break;
            }
        }


        return "Backup of " + file.getFilename() + " has been done with success !";
    }

    private void send_putchunk(Integer replication_degree, String header_end, String sha256hex, MulticastSocket mdb_socket, MulticastSocket mc_socket, int chunk_id, DatagramPacket packet) throws IOException {
        Timer timer = new Timer();

        for (int j = 1; j <= 5; j++) {
            mdb_socket.send(packet);
            final int[] stored_count = {0};

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    byte[] buf = new byte[1000];
                    DatagramPacket received_packet = new DatagramPacket(buf, buf.length);

                    try {
                        mc_socket.receive(received_packet);
                    } catch (IOException e) {
                        System.out.println("Receive MC packet STORED exception : " + e.toString());
                    }
                    String answer = received_packet.getData().toString();
                    String[] fields = answer.split(" ");
                    fields = clean_array(fields);
                    if (fields[0] == "STORED" && fields[1] == "1.0" &&
                            Integer.parseInt(fields[2]) == SERVER_ID &&
                            fields[3] == sha256hex &&
                            Integer.parseInt(fields[4]) == chunk_id &&
                            fields[5] == header_end)
                        stored_count[0]++;
                }
            }, j*1000);

            if (stored_count[0] >= replication_degree)
                break;
        }

        timer.cancel();
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

    public static String[] clean_array(String[] list) {
        List<String> cleaned = new ArrayList<>();

        for (String s: list) {
            if (s.length() > 0)
                cleaned.add(s);
        }

        return cleaned.toArray(new String[0]);
    }

    public static MulticastSocket create_socket(int port, InetAddress group) {
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(port);
            socket.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return socket;
    }
}
