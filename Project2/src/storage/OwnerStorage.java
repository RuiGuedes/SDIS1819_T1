package storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

class OwnerStorage extends Storage<OwnerFile> {

    OwnerStorage() throws IOException {
        super("owner");
    }

    OwnerStorage(Path ownerDirectory) throws IOException {
        super(ownerDirectory);
    }

    @Override
    OwnerFile valueFromFile(Path file) throws IOException {
        return new OwnerFile(file);
    }

    void storeOwner(List<String> fileMetadata) {

    }
}
