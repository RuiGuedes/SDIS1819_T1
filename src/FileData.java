import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileData {

    private String filename;

    private File file;

    private InputStream stream;

    FileData(String filepath) {
        //TODO Check if is directory or file and initialize all variables
        this.file = new File(filename);
        try {
            this.stream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public InputStream getStream() {
        return stream;
    }

    public String getFilename() {
        return filename;
    }

    public File getFile() {
        return file;
    }
}
