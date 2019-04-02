import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.PriorityQueue;

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

    /**
     * [over_replication_degree , file_id , chunk_no]
     */
    private PriorityQueue<String[]> over_replication;

    Storage(Integer server_id) {
        if (Files.exists(Paths.get("peer" + server_id)))
            load_storage(server_id);
        else
            create_storage(server_id);

        //init_over_replication();
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

    static void write_to_file(File file, byte[] data) {
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
     * Change the system storage
     * @param space New space value
     */
    public void update_storage_space(Integer space) {
        this.space = space;
        while (this.space < this.root.getTotalSpace()) {
            if (over_replication.size() > 0) {
                String[] info = over_replication.poll();
                remove_chunk(info[1], info[2]);
            } else {
                // remove random chunks
            }
        }
    }

    /**
     *
     */
    private void init_over_replication() {
        over_replication = new PriorityQueue<>(new String_array_cmp());
        File[] files_directories = info.listFiles();

        for (File file_directory : files_directories) {
            File[] chunks_info_files = file_directory.listFiles();

            for (File chunk_file : chunks_info_files)
                over_replication.add(new String[]
                        {String.valueOf(get_over_replication(chunk_file)),file_directory.getName(),chunk_file.getName()});
        }
    }

    /**
     * Remove a chunk file of the storage and remove empty folders
     * @param file_id File id
     * @param chunk_no Chunk number
     */
    static void remove_chunk(String file_id, String chunk_no) {
        File backup_file_directory = new File(backup, file_id);
        File info_file_directory = new File(info, file_id);

        new File(backup_file_directory, chunk_no).delete();
        new File(info_file_directory, chunk_no).delete();

        if (backup_file_directory.getTotalSpace() == 0) {
            backup_file_directory.delete();
            info_file_directory.delete();
        }

        // TODO - Remove the following lines: Storage class its only used to manipulate files.
        //Message removed_message =
         //       new Message("REMOVED", Peer.getProtocolVersion(),Peer.getServerId(), file_id, Integer.parseInt(chunk_no),null);
        //Peer.getMC().send_packet(removed_message);
    }

    /**
     * Calculate the over replication of a chunk
     * @param file File with chunk replication information
     * @return over replication degree
     */
    static Integer get_over_replication(File file) {
        String[] info_str = read_from_file(file).split("/");

        return (Integer.valueOf(info_str[0]) - Integer.valueOf(info_str[1]));
    }

    static void create_chunk_info(String file_id, Integer chunk_no, Integer replication_degree) {
        File directory = new File(info, file_id);

        if (!directory.exists())
            directory.mkdirs();

        File file_writer = new File(directory, String.valueOf(chunk_no));

        if(!file_writer.exists()) {
            try {
                file_writer.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        write_to_file(file_writer, 0 + "/" + replication_degree);
    }

    /**
     * Stores information about the replication degree of a certain chunk of file
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     */
    static void store_chunk_info(String file_id, Integer chunk_no) {
        File directory = new File(info, file_id);

        if (!directory.exists())
            directory.mkdirs();

        File file_writer = new File(directory, String.valueOf(chunk_no));

        int curr_replication_degree = Integer.valueOf(read_from_file(file_writer).split("/")[0]) + 1;
        int desired_replication_degree = Integer.valueOf(read_from_file(file_writer).split("/")[1]);
        write_to_file(file_writer, curr_replication_degree + "/" + desired_replication_degree);
    }

    static int read_chunk_info(String file_id, Integer chunk_no) {
        File directory = new File(info, file_id);

        if (!directory.exists())
            return 0;

        File file_reader = new File(directory, String.valueOf(chunk_no));

        if(!file_reader.exists())
            return 0;
        else
            return Integer.valueOf(read_from_file(file_reader).split("/")[0]);
    }

    /**
     * Stores chunk content in backup directory and updates its current replication degree
     * @param file_id File identifier
     * @param chunk_no Chunk identifier
     * @param chunk Chunk content
     */
    static void store_chunk(String file_id, Integer chunk_no, byte[] chunk) {
        File directory = new File(backup, file_id);

        if (!directory.exists())
            directory.mkdirs();

        write_to_file(new File(directory, String.valueOf(chunk_no)), chunk);
        Storage.store_chunk_info(file_id, chunk_no);
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

class String_array_cmp implements Comparator<String[]> {

    public int compare (String[] s1, String[] s2) {
        return s1[0].compareTo(s2[0]);
    }
}