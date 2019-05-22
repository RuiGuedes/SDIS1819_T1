package peer;

import storage.ChunkStorage;
import storage.OwnerFile;
import storage.OwnerStorage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;

/**
 * Responsible for managing various operations regarding files, such as file backup, download and delete
 */
public class FileManager {
    /**
     * Backs up a file to the network
     *
     * @param filePath Path pointing to the file
     * @param chunkConsumer Consumer of a chunk index for each chunk, consumed after a chunk is uploaded
     * @param isShareble Whether a metadata file is to be created or not
     *
     * @throws IOException on error reading the file
     * @throws NoSuchAlgorithmException on error retrieving the hashing algorithm
     */
    public static void backup(String filePath, IntConsumer chunkConsumer, boolean isShareble)
            throws IOException, NoSuchAlgorithmException {

        // TODO Disallow multiple backups maybe? No problem in letting duplicate backups

        final Path file = Paths.get(filePath);
        if (!Files.isRegularFile(file)) throw new FileNotFoundException();

        final ArrayList<String> chunkIds = uploadChunks(file, chunkConsumer);

        final List<String> fileMetadata = new LinkedList<>();
        fileMetadata.add(file.getFileName().toString());
        fileMetadata.add(String.valueOf(Files.size(file)));
        fileMetadata.add(String.join("", chunkIds));

        if (isShareble) {
            Files.write(Paths.get(fileMetadata.get(0) + ".meta"),
                    fileMetadata,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }

        OwnerStorage.store(fileMetadata);
    }

    /**
     * Downloads a file from the network
     *
     * @param filePath Path pointing to the file
     * @param chunkConsumer Consumer of a chunk index for each chunk, consumed after a chunk is downloaded
     *
     * @return whether the download was successful or not
     */
    public static boolean download(String filePath, IntConsumer chunkConsumer) {
        final Path file = Paths.get(filePath);

        if (!Files.isRegularFile(file))
            return false;

        try (BufferedReader bf = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            final String fileName = bf.readLine();
            bf.readLine(); // file length
            final String[] chunkIds = OwnerFile.detachChunks(bf.readLine());

            if (filePath.endsWith(".own") && OwnerFile.validate(bf.readLine(), bf.readLine()))
                return false;

            try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(
                    Paths.get(fileName),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
            )) {
                final int chunkNum = chunkIds.length;

                final ArrayList<CompletableFuture<Void>> chunkPromises = new ArrayList<>(chunkNum);
                for (int i = 0; i < chunkNum; i++) {
                    final int chunkIndex = i;

                    final CompletableFuture<Void> chunkPromise = new CompletableFuture<>();
                    chunkPromises.add(chunkIndex, chunkPromise);

                    // TODO Download Chunk

                    // For now retrieve chunk locally
                    final ByteBuffer chunkData = ChunkStorage.get(chunkIds[i]);
                    afc.write(chunkData, i * Chunk.CHUNK_SIZE, chunkPromise, new CompletionHandler<>() {
                        @Override
                        public void completed(Integer result, CompletableFuture<Void> attachment) {
                            chunkConsumer.accept(chunkIndex);
                            attachment.complete(null);
                        }

                        @Override
                        public void failed(Throwable exc, CompletableFuture<Void> attachment) {
                            attachment.completeExceptionally(exc);
                        }
                    });
                }

                chunkPromises.forEach(CompletableFuture::join);
                return true;
            }
        } catch (IOException | InterruptedException | ExecutionException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a file from the network
     *
     * @param ownerPath Path pointing to its owner file
     * @param chunkConsumer Consumer of a chunk index for each chunk, consumed after a chunk is downloaded
     *
     * @return whether the download was successful or not
     */
    public static boolean delete(String ownerPath, IntConsumer chunkConsumer) {
        final Path ownerFile = Paths.get(ownerPath);

        if (!Files.isRegularFile(ownerFile))
            return false;

        try (BufferedReader bf = Files.newBufferedReader(ownerFile, StandardCharsets.UTF_8)) {
            bf.readLine();
            bf.readLine(); // file length
            final String[] chunkIds = OwnerFile.detachChunks(bf.readLine());

            if (!OwnerFile.validate(bf.readLine(), bf.readLine()))
                return false;

            final ArrayList<CompletableFuture<Void>> chunkPromises = new ArrayList<>(chunkIds.length);
            for (int i = 0; i < chunkIds.length; i++) {
                final int chunkIndex = i;

                final CompletableFuture<Void> chunkPromise = new CompletableFuture<>();
                chunkPromises.add(chunkIndex, chunkPromise);

                // TODO Delete Chunk

                // For now delete locally then update the complete the promise
                ChunkStorage.delete(chunkIds[i]);

                chunkPromise.whenComplete((v, e) -> chunkConsumer.accept(chunkIndex));
                chunkPromise.complete(null);
            }
            Files.delete(ownerFile);

            chunkPromises.forEach(CompletableFuture::join);
            return true;
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Seperates the file in chunks and uploads them
     *
     * @param file Path pointing to the file
     * @param chunkConsumer Consumer of a chunk index for each chunk, consumed after a chunk is downloaded
     *
     * @return Ordered list of the uploaded chunk's identifiers
     *
     * @throws IOException on error reading the file
     */
    private static ArrayList<String> uploadChunks(Path file, IntConsumer chunkConsumer) throws IOException {
        try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(file, StandardOpenOption.READ)) {
            // -floorDiv(-x, y) = ceil(x / y)
            final int chunkNum = Math.toIntExact(- Math.floorDiv(- Files.size(file), Chunk.CHUNK_SIZE));

            final ArrayList<CompletableFuture<String>> chunkPromises = new ArrayList<>(chunkNum);
            for (int i = 0; i < chunkNum; i++) {
                final int chunkIndex = i;

                final CompletableFuture<String> chunkPromise = new CompletableFuture<>();
                chunkPromises.add(chunkIndex, chunkPromise);

                final ByteBuffer chunkData = ByteBuffer.allocate(Chunk.CHUNK_SIZE);
                afc.read(chunkData, chunkIndex * Chunk.CHUNK_SIZE, chunkPromise, new CompletionHandler<>() {
                    @Override
                    public void completed(Integer result, CompletableFuture<String> attachment) {
                        try {
                            chunkData.flip();
                            final String chunkId = Chunk.generateId(chunkData);

                            // TODO Upload Chunk

                            // Chunks will be backed up locally for testing purposes
                            ChunkStorage.store(chunkId, chunkData);

                            chunkConsumer.accept(chunkIndex);
                            attachment.complete(chunkId);
                        } catch (NoSuchAlgorithmException | IOException | InterruptedException | ExecutionException e) {
                            attachment.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, CompletableFuture<String> attachment) {
                        attachment.completeExceptionally(exc);
                    }
                });
            }

            final ArrayList<String> chunkIds = new ArrayList<>(chunkNum);
            chunkPromises.forEach((p) -> chunkIds.add(p.join()));
            return chunkIds;
        }
    }

    /**
     * Responsible for handling chunk specific operations
     */
    public static class Chunk {
        public static final int CHUNK_SIZE = 264144;

        /**
         * Generates a chunk identifier given a chunk's content.
         *
         * The identifier is a SHA-256 hash using the chunk's content as input
         *
         * @param dataBuffer Buffer containing a chunk's content
         *
         * @return The chunk's identifier
         *
         * @throws NoSuchAlgorithmException on error retrieving the hashing algorithm
         */
        static String generateId(ByteBuffer dataBuffer) throws NoSuchAlgorithmException {
            final byte[] data = new byte[dataBuffer.remaining()];
            dataBuffer.get(data);
            dataBuffer.rewind();

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return String.format("%064x", new BigInteger(1, sha256.digest(data)));
        }
    }
}
