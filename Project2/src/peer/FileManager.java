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

public class FileManager {
    public static void backup(String filePath, IntConsumer chunkConsumer, boolean isShareble)
            throws IOException, NoSuchAlgorithmException {
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

        OwnerStorage.storeOwner(fileMetadata);
    }

    public static boolean download(String filePath, IntConsumer chunkConsumer) {
        final Path file = Paths.get(filePath);

        if (!Files.isRegularFile(file)) return false;

        try (BufferedReader bf = Files.newBufferedReader(file, StandardCharsets.UTF_8)){
            final String fileName = bf.readLine();
            bf.readLine(); // file length
            final String[] chunkIds = OwnerFile.detachChunks(bf.readLine());

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
                    final ByteBuffer chunkData = ChunkStorage.getChunk(chunkIds[i]);
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
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

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
                            ChunkStorage.storeChunk(chunkId, chunkData);

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

    public static class Chunk {
        public static final int CHUNK_SIZE = 264144;

        static String generateId(ByteBuffer dataBuffer) throws NoSuchAlgorithmException {
            final byte[] data = new byte[dataBuffer.remaining()];
            dataBuffer.get(data);
            dataBuffer.rewind();

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return String.format("%064x", new BigInteger(1, sha256.digest(data)));
        }
    }
}
