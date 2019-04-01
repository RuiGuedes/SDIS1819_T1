import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private static File info;

    /**
     * Local storage file
     */
    private File local_storage;

    /**
     * Local space to storage in bytes
     */
    private long space;

    Storage(Integer server_id) {
        if (Files.exists(Paths.get("peer" + server_id)))
            load_storage(server_id);
        else
            create_storage(server_id);
    }

    /**
     * Loads current storage information
     * @param server_id Server identifier
     */
    private void load_storage(Integer server_id) {
        this.root = new File("peer" + server_id);
        backup = new File(this.root, "backup");
        restore = new File(this.root, "restore");
        info = new File(this.root,"info");
        this.local_storage = new File(this.root, "local_storage.txt");
        this.read_local_storage();
    }

    /**
     * Creates storage structure for the respective server
     * @param server_id Server identifier
     */
    private void create_storage(Integer server_id) {
        this.root = new File("peer" + server_id); this.root.mkdirs();
        backup = new File(this.root, "backup"); backup.mkdirs();
        restore = new File(this.root, "restore"); restore.mkdirs();
        info = new File(this.root, "info"); info.mkdirs();
        this.local_storage = new File(this.root, "local_storage.txt");
        this.space = this.root.getFreeSpace();
        this.write_local_storage(-1);
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
     * @param new_local_storage Peer new local storage value. If value equals -1 it saves host free disk space
     */
    private void write_local_storage(long new_local_storage) {
        try {
            // Opens file, writes respective content and closes it
            synchronized (this.local_storage) {
                BufferedWriter file_writer = new BufferedWriter(new FileWriter(this.local_storage));
                if(new_local_storage == -1)
                    file_writer.write(String.valueOf(this.space));
                else
                    file_writer.write(String.valueOf(new_local_storage));
                file_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to a certain file
     * @param file File where data will be stored
     * @param data Data to be written
     */
    static void write_to_file(File file, String data) {
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
     * Reads data from a file
     * @param file File where data will be read
     * @return File data
     */
    static String read_from_file(File file) {
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
     * Stores information about the replication degree of a certain chunk of file
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param replication_degree Desired replication degree
     */
    static void store_count_messages(String file_id, Integer chunk_no, Integer replication_degree) {
        File directory = new File(info, file_id);
        int curr_replication_degree = 1;

        if (!directory.exists())
            directory.mkdirs();
        else
            curr_replication_degree = Integer.valueOf(read_from_file(new File(directory, String.valueOf(chunk_no))).split("/")[0]) + 1;

        write_to_file(new File(directory, String.valueOf(chunk_no)), curr_replication_degree + "/" + replication_degree);
    }

    static int read_count_messages(String file_id, Integer chunk_no) {
        File directory = new File(info, file_id);

        if (!directory.exists())
            return 0;
        else
            return Integer.valueOf(read_from_file(new File(directory, String.valueOf(chunk_no))).split("/")[0]);
    }

    /**
     * Stores chunk content in backup directory
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param chunk Chunk content
     */
    static void store_chunk(String file_id, Integer chunk_no, String chunk) {
        File directory = new File(backup, file_id);

        if (!directory.exists())
            directory.mkdirs();

        write_to_file(new File(directory, String.valueOf(chunk_no)), chunk);
    }

    /**
     * Reads chunk content if exists
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @return Chunk content
     */
    static String read_chunk(String file_id, Integer chunk_no) {
        Path path = Paths.get(backup.getPath(), file_id, String.valueOf(chunk_no));

        if (!Files.exists(path))
            return null;
        else
            return read_from_file(path.toFile());
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
