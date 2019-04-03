import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Storage {

    /**
     * Root directory [peerX]
     */
    private File root;

    /**
     * Backup directory
     */
    private static File backup;

    /**
     * Restore directory
     */
    private static File restore;

    /**
     * Directory of information about confirm messages
     */
    private static File chunks_info;

    /**
     * Directory of information about backed up files
     */
    private static File backup_info;

    /**
     * Local storage file
     */
    private File local_storage;

    /**
     * Local space to storage in bytes
     */
    private long space;

    /**
     * [over_replication_degree , file_id , chunk_no]
     */
    private PriorityQueue<String[]> chunks_replication;


    private Map<String, String> backed_up_files;

    /**
     * Storage class constructor
     */
    Storage(Integer server_id) {
        if (Files.exists(Paths.get("peer" + server_id)))
            load_storage(server_id);
        else
            create_storage(server_id);

        load_replication();
    }

    /**
     * Writes data to a certain file
     * @param file File where data will be stored
     * @param data Data to be written
     */
    private static void write_to_file(File file, String data) {
        try {
            // Opens file, writes its respective content and closes it
            synchronized (file) {
                FileWriter file_writer = new FileWriter(file);
                file_writer.write(data);
                file_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes bytes to a certain file
     * @param file File where data will be stored
     * @param data Data to be written
     */
    private static void write_to_file(File file, byte[] data) {
        try {
            // Opens file, writes its respective content and closes it
            synchronized (file) {
                FileOutputStream file_writer = new FileOutputStream(file);
                file_writer.write(data);
                file_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads data from a certain file
     * @param file File where data will be read
     * @return File data
     */
    private static String read_from_file(File file) {
        StringBuilder data = new StringBuilder();
        try {
            BufferedReader file_reader = new BufferedReader(new FileReader(file));

            String curr_line;
            while ((curr_line = file_reader.readLine()) != null) {
                data.append(curr_line);
            }

            file_reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data.toString();
    }

    /**
     * Reads bytes from a certain file
     * @param file File where data will be read
     * @return File data
     */
    private static byte[] read_bytes_from_file(File file) {
        byte[] chunk = new byte[64000];
        int bytes_read = 0;
        try {
            FileInputStream file_reader = new FileInputStream(file);
            bytes_read = file_reader.readNBytes(chunk,0,64000);

            file_reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Arrays.copyOf(chunk,bytes_read);
    }

    /**
     * Loads current storage information
     * @param server_id Server identifier
     */
    private void load_storage(Integer server_id) {
        this.root = new File("peer" + server_id);
        backup = new File(this.root, "backup");
        restore = new File(this.root, "restore");
        chunks_info = new File(this.root,"chunks_info");
        backup_info = new File(this.root, "backup_info");
        this.local_storage = new File(this.root, "local_storage.txt");
        this.read_local_storage();
        this.read_backed_up_files();
    }

    /**
     * Creates storage structure for the respective server
     * @param server_id Server identifier
     */
    private void create_storage(Integer server_id) {
        this.root = new File("peer" + server_id); this.root.mkdirs();
        backup = new File(this.root, "backup"); backup.mkdirs();
        restore = new File(this.root, "restore"); restore.mkdirs();
        chunks_info = new File(this.root, "chunks_info"); chunks_info.mkdirs();
        backup_info = new File(this.root, "backup_info"); backup_info.mkdirs();
        this.local_storage = new File(this.root, "local_storage.txt");
        this.space = this.root.getFreeSpace();
        this.write_local_storage();
        this.backed_up_files = new HashMap<>();
    }

    /**
     * Reads peer local storage
     */
    private void read_local_storage() {
        try {
            // Opens file, reads local storage and closes file
            BufferedReader file_reader = new BufferedReader(new FileReader(this.local_storage));
            this.space = Long.parseLong(file_reader.readLine());
            file_reader.close();

            // Checks if the current local system running the peer has lower memory than required
            if(this.space > this.root.getFreeSpace())
                this.space = this.root.getFreeSpace();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Writes local storage to its respective file
     */
    private void write_local_storage() {
        try {
            // Opens file, writes respective content and closes it
            synchronized (this.local_storage) {
                BufferedWriter file_writer = new BufferedWriter(new FileWriter(this.local_storage));
                file_writer.write(String.valueOf(this.space));
                file_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Change the system storage
     * @param space New space value
     */
    void set_storage_space(Integer space) {
        this.space = space;
        while (this.space < this.root.getTotalSpace()) {
            if (chunks_replication.size() > 0) {
                String[] info = chunks_replication.poll();
                delete_chunk(info[1], Integer.parseInt(info[2]));
            }
        }

        write_local_storage();
    }

    /**
     * Update the system storage
     * @param space Space to increase/decrease
     */
    public void update_storage_space(Integer space) {
        this.space += space;
        write_local_storage();
    }

    /**
     * Get the space available to store data
     * @return Free space
     */
    int get_free_space() {
        return (int) (this.space - this.root.getTotalSpace());
    }

    /**
     * Reads information about backed up files
     */
    private void read_backed_up_files() {
        this.backed_up_files = new HashMap<>();

        for (final File file_entry : Objects.requireNonNull(backup_info.listFiles())) {
            if (!file_entry.isDirectory()) {
                try {
                    // Opens file, reads local storage and closes file
                    BufferedReader file_reader = new BufferedReader(new FileReader(file_entry));
                    this.backed_up_files.put(file_entry.getName(), file_reader.readLine());
                    file_reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Adds file to backed up files and stores it in the filesystem
     * @param file File to be stored
     */
    public void store_backed_up_file(FileData file) {
        this.backed_up_files.put(file.get_filename(), file.get_file_id());

        try {
            File file_writer = new File(backup_info, file.get_filename());
            file_writer.createNewFile();
            write_to_file(file_writer, file.get_file_id());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove_backed_up_file(FileData file) {
        this.backed_up_files.remove(file.get_filename());
        new File(backup_info, file.get_filename()).delete();
    }

    /**
     * Check if file is backed up or not
     * @param file File to be checked
     * @return True if it has, false otherwise.
     */
    public String is_backed_up(FileData file) {
        if(!this.backed_up_files.isEmpty() & this.backed_up_files.containsKey(file.get_filename())) {
            if(this.backed_up_files.get(file.get_filename()).equals(file.get_file_id()))
                return "RETURN";
            else
                return "DELETE-AND-BACKUP";
        }
        else
            return "BACKUP";
    }


    /**
     *
     */
    private void load_replication() {
        this.chunks_replication = new PriorityQueue<>(new String_array_cmp());
        File[] files_directories = chunks_info.listFiles();

        for (File file_directory : files_directories) {
            File[] chunks_info_files = file_directory.listFiles();

            for (File chunk_file : chunks_info_files)
                this.chunks_replication.add(new String[]
                        {String.valueOf(get_chunk_replication(chunk_file)),file_directory.getName(),chunk_file.getName()});
        }
    }

    /**
     * Calculate the over replication of a chunk
     * @param file File with chunk replication information
     * @return over replication degree
     */
    static Integer get_chunk_replication(File file) {
        String[] info_str = read_from_file(file).split("/");

        return (Integer.valueOf(info_str[0]) - Integer.valueOf(info_str[1]));
    }





    static void create_chunk_info(String file_id, Integer chunk_no, Integer replication_degree) {
        File directory = new File(chunks_info, file_id);
        directory.mkdirs();

        File file_writer = new File(directory, String.valueOf(chunk_no));

        try {
            file_writer.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        write_to_file(file_writer, 0 + "/" + replication_degree);
    }

    /**
     * Stores information about the replication degree of a certain chunk of file
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param increment Value to increment (positive or negative)
     */
    static boolean store_chunk_info(String file_id, Integer chunk_no, Integer increment) {
        File directory = new File(chunks_info, file_id);
        directory.mkdirs();

        File file_writer = new File(directory, String.valueOf(chunk_no));

        int curr_replication_degree = Integer.valueOf(read_from_file(file_writer).split("/")[0]) + increment;
        int desired_replication_degree = Integer.valueOf(read_from_file(file_writer).split("/")[1]);
        write_to_file(file_writer, curr_replication_degree + "/" + desired_replication_degree);

        return curr_replication_degree >= desired_replication_degree;
    }

    static int read_chunk_info(String file_id, Integer chunk_no) {
        File directory = new File(chunks_info, file_id);

        if (!directory.exists())
            return 0;

        File file_reader = new File(directory, String.valueOf(chunk_no));

        if(!file_reader.exists())
            return 0;
        else
            return Integer.valueOf(read_from_file(file_reader).split("/")[0]);
    }

    static int read_chunk_info_replication(String file_id, Integer chunk_no) {
        File directory = new File(chunks_info, file_id);

        if (!directory.exists())
            return 0;

        File file_reader = new File(directory, String.valueOf(chunk_no));

        if(!file_reader.exists())
            return 0;
        else
            return Integer.valueOf(read_from_file(file_reader).split("/")[1]);
    }

    public static boolean exists_file(String file_id) {
        return new File(backup, file_id).exists();
    }

    public static void delete_file(String file_id) {
        File backup_file_directory = new File(backup, file_id);
        File info_file_directory = new File(chunks_info, file_id);

        for (File chunk : backup_file_directory.listFiles()) {
            chunk.delete();
        }
        backup_file_directory.delete();

        for (File chunk : info_file_directory.listFiles()) {
            chunk.delete();
        }
        info_file_directory.delete();
    }

    static  boolean exists_chunk(String file_id, Integer chunk_no) {
        File file_directory = new File(backup,file_id);

        if (!file_directory.exists())
            return false;

        if (new File(file_directory, String.valueOf(chunk_no)).exists())
            return true;

        return false;
    }

    /**
     * Stores chunk content in backup directory and updates its current replication degree
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param chunk Chunk content
     */
    static void store_chunk(String file_id, Integer chunk_no, byte[] chunk) {
        File directory = new File(backup, file_id);
        directory.mkdirs();

        write_to_file(new File(directory, String.valueOf(chunk_no)), chunk);
        Storage.store_chunk_info(file_id, chunk_no, 1);
    }

    /**
     * Reads chunk content if exists
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @return Chunk content
     */
    static byte[] read_chunk(String file_id, Integer chunk_no) {
        Path path = Paths.get(backup.getPath(), file_id, String.valueOf(chunk_no));

        return read_bytes_from_file(path.toFile());
    }

    /**
     * Remove a chunk file of the storage and remove empty folders
     * @param file_id File id
     * @param chunk_no Chunk number
     */
    static void delete_chunk(String file_id, Integer chunk_no) {
        File backup_file_directory = new File(backup, file_id);
        File info_file_directory = new File(chunks_info, file_id);

        // Check if directories exist
        if (!backup_file_directory.exists() || !info_file_directory.exists())
            return;

        // Delete chunk files
        new File(backup_file_directory, String.valueOf(chunk_no)).delete();
        new File(info_file_directory, String.valueOf(chunk_no)).delete();

        // Delete directory if empty
        if (backup_file_directory.getTotalSpace() == 0) {
            backup_file_directory.delete();
            info_file_directory.delete();
        }

        Peer.getMC().send_packet(
                new Message("REMOVED", Peer.get_protocol_version(),Peer.get_server_id(), file_id, chunk_no,null, null));
    }

    // Restore file receiving chunks array ( or MAP<chunk_no, chunk> ??)
    static void restore_file(String filename, String[] chunks) {
        File restored_file = new File(restore,filename);
        try {
            FileWriter file = new FileWriter(restored_file);

            for (int i = 0; i< chunks.length ; i++) {
                file.write(chunks[i]);
            }
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/**
 * Priority queue comparator
 */
class String_array_cmp implements Comparator<String[]> {
    public int compare (String[] s1, String[] s2) {
        return s1[0].compareTo(s2[0]);
    }
}