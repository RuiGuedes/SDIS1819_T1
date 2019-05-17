package middleware;

import storage.FileManager;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestListener implements Runnable {
    private final static ExecutorService requestPool = Executors.newCachedThreadPool();
    private final SSLServerSocket serverSocket;

    public RequestListener(int port) throws IOException {
        final SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                requestPool.submit(new RequestHandler((SSLSocket) serverSocket.accept()));
            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private class RequestHandler implements Runnable {
        private final SSLSocket requestSocket;

        RequestHandler(SSLSocket requestSocket) {
            this.requestSocket = requestSocket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(this.requestSocket.getInputStream()));
                PrintWriter out = new PrintWriter(this.requestSocket.getOutputStream(), true)
            ){
                final String[] command = in.readLine().split(" ");
                final String[] commandArgs = Arrays.copyOfRange(command, 1, command.length);

                switch(command[0]) {
                    case "BACKUP":
                        if (!backupCommand(commandArgs, out))
                            out.println("Invalid command: " + String.join(" ", command));
                        break;
                    default:
                        out.println("Invalid command: " + String.join(" ", command));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean backupCommand(String[] commandArgs, PrintWriter out) {
            if (commandArgs.length > 2)     return false;

            String filePath = null;
            boolean isShareble = false;

            for (String arg : commandArgs) {
                if (!isShareble && (arg.equals("--share") || arg.equals("-s")))
                    isShareble = true;
                else if (filePath == null && (!arg.startsWith("-")))
                    filePath = arg;
                else
                    return false;
            }

            ArrayList<CompletableFuture<String>> chunkUploads = FileManager.backup(filePath);

            if (chunkUploads == null)   return false;

            out.println(chunkUploads.size() + " chunks");

            for (int i = 0; i < chunkUploads.size(); i++) {
                final int chunkIndex = i;
                chunkUploads.get(i).whenComplete((chunkId, e) -> {
                    // TODO Retrieve chunkId for files

                    out.println(chunkIndex);
                });
            }

            CompletableFuture.allOf(chunkUploads.toArray(new CompletableFuture[0])).whenComplete((r, e) -> {
                // TODO create ownerfile and metadata file

                out.println("Backup successful.");
            }).join();

            return true;
        }
    }
}
