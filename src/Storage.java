import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Storage {

    // TODO - Update local storage on file
    // TODO - Space is only relative to chunk storage not all files involved

    /**
     * Chunk information size
     */
    static int CHUNK_INFO_SIZE = 3;

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
     * Structure containing chunks replication degree: [over_replication_degree , file_id , chunk_no]
     */
    private PriorityQueue<String[]> chunks_replication;

    /**
     * Structure that contains all restoring files stats
     */
    volatile static Map<String, Map<Integer, byte[]>> files_to_restore = new HashMap<>();

    /**
     * Structure that contains all chunk information
     */
    volatile static Map<String, Map<Integer, Integer>> chunks_info_struct = new HashMap<>();

    /**
     * Structure containing backed up files information
     */
    private Map<String, String> backed_up_files;

    /**
     * Storage class constructor
     */
    Storage(Integer server_id) {
        if (Files.exists(Paths.get("../../peers/peer" + server_id)))
            load_storage(server_id);
        else
            create_storage(server_id);
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
            synchronized (file) {
                BufferedReader file_reader = new BufferedReader(new FileReader(file));

                String curr_line;
                while ((curr_line = file_reader.readLine()) != null) {
                    data.append(curr_line);
                }

                file_reader.close();
            }
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
            synchronized (file) {
                FileInputStream file_reader = new FileInputStream(file);
                bytes_read = file_reader.readNBytes(chunk,0,64000);

                file_reader.close();
            }
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
        this.root = new File("../../peers/peer" + server_id);
        backup = new File(this.root, "backup");
        restore = new File(this.root, "restore");
        chunks_info = new File(this.root,"chunks_info");
        backup_info = new File(this.root, "backup_info");
        this.local_storage = new File(this.root, "local_storage.txt");
        this.read_local_storage();
        this.read_backed_up_files();
        this.load_replication();
    }

    /**
     * Creates storage structure for the respective server
     * @param server_id Server identifier
     */
    private void create_storage(Integer server_id) {
        this.root = new File("../../peers/peer" + server_id); this.root.mkdirs();
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
    boolean set_storage_space(Integer space) {
        this.space = space;
        while (this.space < this.root.getTotalSpace()) {
            if (chunks_replication.size() > 0) {
                String[] info = chunks_replication.poll();
                delete_chunk(info[1], Integer.parseInt(info[2]));
            }
        }

        write_local_storage();
        return true;
    }

    //////////////////////////////////////////////

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

    //////////////////////////////////////////////

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
    void store_backed_up_file(FileData file) {
        this.backed_up_files.put(file.get_filename(), file.get_file_id());

        try {
            File file_writer = new File(backup_info, file.get_filename());
            file_writer.createNewFile();
            write_to_file(file_writer, file.get_file_id());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removed backed up file from file system
     * @param filename Name of the file to be removed
     */
    void remove_backed_up_file(String filename) {
        this.backed_up_files.remove(filename);
        new File(backup_info, filename).delete();
    }

    /**
     * Check if file is backed up or not
     * @param filename Filename
     * @param file_id File id
     * @return True if it has, false otherwise.
     */
    String is_backed_up(String filename, String file_id) {
        if(!this.backed_up_files.isEmpty() & this.backed_up_files.containsKey(filename)) {
            if(this.backed_up_files.get(filename).equals(file_id))
                return "RETURN";
            else
                return "DELETE-AND-BACKUP";
        }
        else
            return "BACKUP";
    }

    /**
     * Get file id of backed up file
     * @param filename Filename
     * @return File id
     */
    String get_backed_up_file_id(String filename) {
        return this.backed_up_files.getOrDefault(filename, "");
    }

    /**
     * Deletes a backed up file and its information file
     * @param file_id File id
     */
    static void delete_file(String file_id) {
        File chunk_backup = new File(backup, file_id);
        File chunk_info = new File(chunks_info, file_id);

        // Delete backed up chunks
        if(chunk_backup.exists()) {
            for (File chunk : Objects.requireNonNull(chunk_backup.listFiles())) {
                chunk.delete();
            }
            chunk_backup.delete();
        }

        // Delete files with information about backed up chunks
        if(chunk_info.exists()) {
            for (File chunk : Objects.requireNonNull(chunk_info.listFiles())) {
                chunk.delete();
            }
            chunk_info.delete();
        }
    }


    private void load_replication() {
        this.chunks_replication = new PriorityQueue<>(new String_array_cmp());
        File[] files_directories = chunks_info.listFiles();

        for (File file_directory : Objects.requireNonNull(files_directories)) {
            File[] chunks_info_files = file_directory.listFiles();

            for (File chunk_file : Objects.requireNonNull(chunks_info_files))
                this.chunks_replication.add(new String[]
                        {String.valueOf(get_chunk_over_replication(chunk_file)),file_directory.getName(),chunk_file.getName()});
        }
    }

    /**
     * Creates chunk information file
     * @param file_id File id
     * @param chunk_no Chunk number
     * @param replication_degree Desired replication degree
     */
    static void create_chunk_info(String file_id, Integer chunk_no, Integer replication_degree) {
        if(has_chunk_info(file_id, chunk_no))
            return;

        File directory = new File(chunks_info, file_id);
        directory.mkdirs();

        File file_writer = new File(directory, String.valueOf(chunk_no));

        try {
            file_writer.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        write_to_file(file_writer,  "0/" + replication_degree);
    }

    /**
     * Stores information about the replication degree of a certain chunk of file
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param increment Value to increment (positive or negative)
     */
    static boolean store_chunk_info(String file_id, Integer chunk_no, Integer increment) {

        if (!has_chunk_info(file_id,chunk_no))
            return false;

        File directory = new File(chunks_info, file_id);
        File file_writer = new File(directory, String.valueOf(chunk_no));

        String data = read_from_file(file_writer);
        if(!data.equals("")) {
            int curr_replication_degree = Integer.valueOf(data.split("/")[0]) + increment;
            int desired_replication_degree = Integer.valueOf(data.split("/")[1]);
            write_to_file(file_writer, curr_replication_degree + "/" + desired_replication_degree);

            return curr_replication_degree >= desired_replication_degree;
        }

        return false;
    }

    static void store_chunks_info_of_file(String file_id, Integer replication_degree) {
        for(Map.Entry<Integer, Integer> chunk : chunks_info_struct.get(file_id).entrySet()) {
            create_chunk_info(file_id, chunk.getKey(), replication_degree);
            store_chunk_info(file_id, chunk.getKey(), chunk.getValue());
        }
        chunks_info_struct.remove(file_id);
    }

    /**
     * Reads chunk perceived replication degree
     * @param file_id File id
     * @param chunk_no Chunk number
     * @param replication_type Replication degree type: 0 for perceived replication : 1 for desired replication
     * @return Perceived replication degree
     */
    static int read_chunk_info(String file_id, Integer chunk_no, int replication_type) {
        File directory = new File(chunks_info, file_id);

        if (!directory.exists())
            return 0;

        File file_reader = new File(directory, String.valueOf(chunk_no));

        if(file_reader.exists() && !read_from_file(file_reader).equals(""))
            return Integer.valueOf(read_from_file(file_reader).split("/")[replication_type]);

        return 0;
    }

    static boolean has_chunk_info(String file_id, Integer chunk_no) {
        return  new File(new File(chunks_info, file_id), String.valueOf(chunk_no)).exists();
    }

    static void syncronized_put_chunk_info(String file_id, Integer chunk_no, Integer info) {

        synchronized (chunks_info_struct) {
            chunks_info_struct.get(file_id).put(chunk_no, info);
        }
    }

    static Integer syncronized_get_chunk_info(String file_id, Integer chunk_no) {

        synchronized (chunks_info_struct) {
            if (chunks_info_struct.get(file_id).containsKey(chunk_no))
                return chunks_info_struct.get(file_id).get(chunk_no);
            else{
                chunks_info_struct.get(file_id).put(chunk_no, 0);
                return 0;
            }
        }
    }

    static boolean syncronized_contains_chunk_info(String file_id, Integer chunk_no) {

        synchronized (chunks_info_struct) {
            if (chunk_no == null)
                return chunks_info_struct.containsKey(file_id);
            else {
                if(chunks_info_struct.containsKey(file_id))
                    return chunks_info_struct.get(file_id).containsKey(chunk_no);
                else
                    return false;
            }
        }
    }

    static void syncronized_inc_chunk_info(String file_id, Integer chunk_no) {
        synchronized (chunks_info_struct) {
            Integer old_rep = chunks_info_struct.get(file_id).get(chunk_no) + 1;
            chunks_info_struct.get(file_id).put(chunk_no, old_rep);
        }
    }

    /**
     * Calculate the over replication of a chunk
     * @param file File with chunk replication information
     * @return Over replication degree
     */
    private static Integer get_chunk_over_replication(File file) {
        String[] info_str = read_from_file(file).split("/");

        return (Integer.valueOf(info_str[0]) - Integer.valueOf(info_str[1]));
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
     * Get the number of chunks associated to a certain file
     * @param file_id File id
     * @return Number of associated chunks
     */
    static int get_num_chunks(String file_id) {
        File directory = new File(chunks_info, file_id);

        if(directory.exists())
            return Objects.requireNonNull(directory.listFiles()).length;
        else
            return 0;
    }

    /**
     * Checks if peer backed up a certain chunk
     * @param file_id File id
     * @param chunk_no Chunk number
     * @return True if it contains chunk, false otherwise
     */
    static  boolean has_chunk(String file_id, Integer chunk_no) {
        return new File(new File(backup, file_id), String.valueOf(chunk_no)).exists();
    }

    /**
     * Remove a chunk file of the storage and remove empty folders
     * @param file_id File id
     * @param chunk_no Chunk number
     */
    private static void delete_chunk(String file_id, Integer chunk_no) {
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

    /**
     * Restores file with a given name and id
     * @param filename Filename
     * @param file_id File id
     */
    void restore_file(String filename, String file_id) {
        File file = new File(restore,filename);

        // Delete file if it exists
        if(file.exists())
            file.delete();

        try {
            // Creates file
            file.createNewFile();
            FileOutputStream file_writer = new FileOutputStream(file, true);
            Map<Integer, byte[]> data = files_to_restore.get(file_id);

            // Writes content to file
            for(Map.Entry<Integer, byte[]> chunk : data.entrySet()) {
                try {
                    synchronized (file) {
                        file_writer.write(chunk.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String state() {
        String peer_state = ":::::::::::::::::::::\n"
                          + ":: BACKED UP FILES ::\n"
                          + ":::::::::::::::::::::\n\n";

        if(backup_info.exists()) {
            if(Objects.requireNonNull(backup_info.listFiles()).length == 0)
                peer_state += "No backed up files\n\n";
            else {
                for (File file : Objects.requireNonNull(backup_info.listFiles())) {
                    String pathname = file.getPath();
                    String file_id = read_from_file(file);
                    int desired_replication_degree = read_chunk_info(file_id, 0, 1);

                    peer_state += "File Pathname: " + pathname + "\n"
                            + "Backup Service ID: " + file_id + "\n"
                            + "Desired Replication Degree: " + desired_replication_degree + "\n"
                            + "Stored Chunks:\n";

                    File[] chunk_files = new File(chunks_info, file_id).listFiles();
                    Arrays.sort(Objects.requireNonNull(chunk_files), Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

                    for (File chunk : chunk_files) {
                        String id = chunk.getName();
                        String replication_degree = String.valueOf(read_chunk_info(file_id, Integer.parseInt(id), 0));
                        peer_state += "-> Chunk number " + id + " with perceived replication degree of " + replication_degree + "\n";
                    }
                    peer_state += "\n";
                }
            }
        }

        peer_state += ":::::::::::::::::::\n"
                    + ":: CHUNKS STORED ::\n"
                    + ":::::::::::::::::::\n\n";

        if(backup.exists()) {
            if(Objects.requireNonNull(backup.listFiles()).length == 0)
                peer_state += "No chunks stored\n\n";
            else {
                for (File file : Objects.requireNonNull(backup.listFiles())) {
                    String file_id = file.getName();

                    peer_state += "File ID: " + file_id + "\nStored Chunks:\n";

                    File[] chunk_files = new File(backup, file_id).listFiles();
                    Arrays.sort(Objects.requireNonNull(chunk_files), Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

                    for (File chunk : chunk_files) {
                        String id = chunk.getName();
                        String size = String.valueOf(chunk.length());
                        String replication_degree = String.valueOf(read_chunk_info(file_id, Integer.parseInt(id), 0));

                        peer_state += "-> Chunk number " + id + " with " + size + " bytes has a perceived replication degree of " + replication_degree + "\n";
                    }
                    peer_state += "\n";
                }
            }
        }

        peer_state += ":::::::::::::\n"
                    + ":: STORAGE ::\n"
                    + ":::::::::::::\n\n";

        // TODO - After local storage is well defined
        // The peer's storage capacity, i.e. the maximum amount of disk space that can be used to store chunks, and the amount of storage (both in KBytes) used to backup the chunks.

        return peer_state;
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