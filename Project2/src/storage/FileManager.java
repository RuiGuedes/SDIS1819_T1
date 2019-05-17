package storage;

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

public class FileManager {
    public static ArrayList<CompletableFuture<String>> backup(String filePath) {
        final Path backupFile = Paths.get(filePath);

        if (!Files.isRegularFile(backupFile)) return null;

        try {
            return UploadChunks(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean createOwner(String filePath, ArrayList<String> chunkIds, boolean isShareble) {
        final Path backupFile = Paths.get(filePath);

        if (!Files.isRegularFile(backupFile)) return false;

        try {
            final List<String> fileMetadata = new LinkedList<>();
            fileMetadata.add(backupFile.getFileName().toString());
            fileMetadata.add(String.valueOf(Files.size(backupFile)));
            fileMetadata.addAll(chunkIds);

            StorageManager.getOwnerStorage().storeOwner(fileMetadata);

            if (isShareble) {
                Files.write(backupFile.getFileName(), fileMetadata, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private static ArrayList<CompletableFuture<String>> UploadChunks(Path backupFile) throws IOException {
        try (AsynchronousFileChannel fc = AsynchronousFileChannel.open(backupFile, StandardOpenOption.READ)) {
            // -floorDiv(-x, y) = ceil(x / y)
            final int chunkNum = Math.toIntExact(- Math.floorDiv(- Files.size(backupFile), Chunk.CHUNK_SIZE));

            ArrayList<CompletableFuture<String>> chunkIds = new ArrayList<>(chunkNum);
            for (int i = 0; i < chunkNum; i++) {
                final CompletableFuture<String> chunkUpload = new CompletableFuture<>();
                chunkIds.add(i, chunkUpload);

                final ByteBuffer chunkData = ByteBuffer.allocate(Chunk.CHUNK_SIZE);
                fc.read(chunkData, i * Chunk.CHUNK_SIZE, chunkUpload, new CompletionHandler<>() {
                    @Override
                    public void completed(Integer result, CompletableFuture<String> attachment) {
                        try {
                            chunkData.flip();
                            final String chunkId = Chunk.generateId(chunkData);

                            // TODO Upload Chunk

                            // Chunks will be backed up locally for testing purposes
                            StorageManager.getChunkStorage().storeChunk(chunkId, chunkData);

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

            return chunkIds;
        }
    }

    private static class Chunk {
        static int CHUNK_SIZE = 264144;

        static String generateId(ByteBuffer dataBuffer) throws NoSuchAlgorithmException {
            final byte[] data = new byte[dataBuffer.remaining()];
            dataBuffer.get(data);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(data);

            return String.format("%064x", new BigInteger(1, digest));
        }
    }
}
