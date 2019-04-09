import java.util.HashMap;
import java.util.Map;

class Synchronized {

    /**
     * Structure that contains all restoring files stats
     */
    volatile static Map<String, Map<Integer, byte[]>> files_to_restore = new HashMap<>();

    /**
     * Structure that contains all chunk information
     */
    volatile static Map<String, Map<Integer, Integer>> chunks_info_struct = new HashMap<>();

    /**
     * Structure that contains all chunk information
     */
    private volatile static Map<String, Map<Integer, Integer>> stored_messages = new HashMap<>();

    /**
     * Synchronized access to chunk info structure to put data
     * @param file_id File ID
     * @param chunk_no Chunk number
     * @param info Data
     */
    static void synchronized_put_chunk_info(String file_id, Integer chunk_no, Integer info) {
        synchronized (chunks_info_struct) {
            chunks_info_struct.get(file_id).put(chunk_no, info);
        }
    }

    /**
     * Synchronized access to chunk info structure to get data
     * @param file_id File ID
     * @param chunk_no Chunk number
     * @return Replication degree
     */
    static Integer synchronized_get_chunk_info(String file_id, Integer chunk_no) {
        synchronized (chunks_info_struct) {
            if (chunks_info_struct.get(file_id).containsKey(chunk_no))
                return chunks_info_struct.get(file_id).get(chunk_no);
            else
                return 0;
        }
    }

    /**
     * Synchronized access to chunk info structure to check elements presence
     * @param file_id File ID
     * @param chunk_no Chunk number
     * @return True if it contains false otherwise
     */
    static boolean synchronized_contains_chunk_info(String file_id, Integer chunk_no) {
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

    /**
     * Synchronized access to chunk info structure to update elements by incrementing their value
     * @param file_id File ID
     * @param chunk_no Chunk number
     */
    static void synchronized_inc_chunk_info(String file_id, Integer chunk_no) {
        synchronized (chunks_info_struct) {
            Integer new_rep = chunks_info_struct.get(file_id).get(chunk_no) + 1;
            chunks_info_struct.get(file_id).put(chunk_no, new_rep);
        }
    }

    /**
     * Synchronized access to files to restore structure to check elements
     * @param file_id File ID
     * @param chunk_no Chunk number
     * @return True if it contains false otherwise
     */
    static boolean synchronized_contains_files_to_restore(String file_id, Integer chunk_no) {
        synchronized (files_to_restore) {
            if(chunk_no == null)
                return files_to_restore.containsKey(file_id);
            else {
                if(files_to_restore.containsKey(file_id))
                    return files_to_restore.get(file_id).containsKey(chunk_no);
                else
                    return false;
            }
        }
    }

    /**
     * Synchronized access to files to restore structure to check for null elements
     * @param file_id File ID
     * @return True if it contains false otherwise
     */
    static boolean synchronized_contains_null_value(String file_id) {
        synchronized (files_to_restore) {
            if(files_to_restore.containsKey(file_id)) {
                 for(Map.Entry<Integer, byte[]> data : files_to_restore.get(file_id).entrySet()) {
                     if(data.getValue() == null)
                         return true;
                 }
                 return false;
            }
            else
                return false;
        }
    }

    /**
     * Synchronized access to files to restore structure to put elements
     * @param file_id File ID
     * @param chunk_no Chunk number
     */
    static void synchronized_put_files_to_restore(String file_id, Integer chunk_no, byte[] chunk_body) {
        synchronized (files_to_restore) {
            if(chunk_no == null)
                files_to_restore.put(file_id, new HashMap<>());
            else {
                if(files_to_restore.containsKey(file_id))
                    files_to_restore.get(file_id).put(chunk_no, chunk_body);
            }
        }
    }

    /**
     * Synchronized access to files to restore structure to remove elements
     * @param file_id File ID
     */
    static void synchronized_remove_files_to_restore(String file_id) {
        synchronized (files_to_restore) {
            files_to_restore.remove(file_id);
        }
    }

    /**
     * Synchronized access to files to restore structure to get its size
     * @param file_id File ID
     */
    static int synchronized_size_files_to_restore(String file_id) {
        synchronized (files_to_restore) {
            if(files_to_restore.containsKey(file_id))
                return files_to_restore.get(file_id).size();
            else
                return 0;
        }
    }

    /**
     * Synchronized access to stored_messages structure to get data
     * @param file_id File ID
     * @param chunk_no Chunk number
     * @return Replication degree
     */
    static Integer synchronized_get_stored_message(String file_id, Integer chunk_no) {
        synchronized (stored_messages) {
            if(!stored_messages.containsKey(file_id)) {
                stored_messages.put(file_id, new HashMap<>());
                return 0;
            }
            else {
                if (stored_messages.get(file_id).containsKey(chunk_no))
                    return stored_messages.get(file_id).get(chunk_no);
                else{
                    stored_messages.get(file_id).put(chunk_no, 0);
                    return 0;
                }
            }
        }
    }

    /**
     * Synchronized access to stored_messages structure to update elements by incrementing their value
     * @param file_id File ID
     * @param chunk_no Chunk number
     */
    static void synchronized_inc_stored_message(String file_id, Integer chunk_no) {
        synchronized (stored_messages) {
            if(!stored_messages.containsKey(file_id))
                stored_messages.put(file_id, new HashMap<>());

            int new_rep = 1;

            if (stored_messages.get(file_id).containsKey(chunk_no))
                new_rep = stored_messages.get(file_id).get(chunk_no) + 1;

            stored_messages.get(file_id).put(chunk_no, new_rep);
        }
    }


}
