package storage;

import java.io.IOException;
import java.nio.file.Path;

class OwnerStorage extends Storage<OwnerFile> {

    OwnerStorage() {
        super("owner");
    }

    OwnerStorage(Path ownerDirectory) throws IOException {
        super(ownerDirectory);
    }

    @Override
    OwnerFile ValueFromFile(Path file) throws IOException {
        return new OwnerFile(file);
    }
}
