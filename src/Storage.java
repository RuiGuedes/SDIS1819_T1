import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Storage {

    /**
     * Root directory (peerX)
     */
    private File root;

    /**
     * Backup directory
     */
    private File backup;

    /**
     * Restore directory
     */
    private File restore;

    /**
     * Directory of information about confirm messages
     */
    private File info;

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
        this.backup = new File(this.root, "backup");
        this.restore = new File(this.root, "restore");
        this.local_storage = new File(this.root, "local_storage.txt");
        this.info = new File(this.root,"info");
        this.read_local_storage();
    }

    /**
     * Creates storage structure for the respective server
     * @param server_id Server identifier
     */
    private void create_storage(Integer server_id) {
        this.root = new File("peer" + server_id); this.root.mkdirs();
        this.backup = new File(this.root, "backup"); this.backup.mkdirs();
        this.restore = new File(this.root, "restore"); this.restore.mkdirs();
        this.info = new File(this.root, "info"); this.info.mkdirs();
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
            FileWriter file_writer = new FileWriter(this.local_storage);
            if(new_local_storage == 1)
                file_writer.write(String.valueOf(this.space));
            else
                file_writer.write(String.valueOf(new_local_storage));
            file_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Store num confirm message
    public void store_count_messages(String file_id, Integer chunk_no, Integer count) {
        File file_directory = new File(this.info, file_id);

        if (!file_directory.exists())
            file_directory.mkdirs();
        write_in_chunk_file(file_directory, chunk_no, String.valueOf(count));
    }

    // Store Chunk
    public void store_chunk(String file_id, Integer chunk_no, String chunk) {
        File file_directory = new File(this.backup, file_id);

        if (!file_directory.exists())
            file_directory.mkdirs();
        write_in_chunk_file(file_directory, chunk_no, chunk);
    }

    private void write_in_chunk_file(File file_directory, Integer chunk_no, String to_store) {
        File chunk_file = new File(file_directory, String.valueOf(chunk_no));

        try {
            chunk_file.createNewFile();
            new FileWriter(chunk_file).write(to_store);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read Chunk if exists
    public String read_chunk(String file_id, Integer chunk_no) {
        Path path = Paths.get(this.backup.getPath(), file_id, String.valueOf(chunk_no));
        byte[] chunk = new byte[0];

        if (!Files.exists(path))
            return null;

        try {
            FileInputStream chunk_file = new FileInputStream(path.toFile());
            chunk = chunk_file.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String chunk_str = "";
        try {
             chunk_str = new String(chunk,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return chunk_str;
    }

    // Restore file receiving chunks array ( or MAP<chunk_no, chunk> ??)
    public void restore_file(String filename, String[] chunks) {
        File restored_file = new File(this.restore,filename);
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

    public static void main(String[] args) {
        Storage s = new Storage(1);
        System.out.println("s created");

        s.store_chunk("as",1,"pppppp");
        System.out.println("chunk stored");

        s.store_count_messages("as",1,4);
        System.out.println("count stored");

        System.out.println("chunk " + s.read_chunk("as", 1));

        s.restore_file("as", new String[]{"asdc", "ppsds"});
        System.out.println("file restored");
    }
}
