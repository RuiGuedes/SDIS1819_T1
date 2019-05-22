package middleware;

import peer.FileManager;
import storage.ChunkStorage;
import storage.OwnerStorage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
                final String[] command = in.readLine().split("\\|");
                final String[] commandArgs = Arrays.copyOfRange(command, 1, command.length);
                final String commandArgsString = String.join(" ", commandArgs);

                switch(command[0]) {
                    case "BACKUP":
                        if (!backupCommand(commandArgs, out))
                            out.println("Invalid BACKUP command: " + commandArgsString);
                        break;
                    case "DOWNLOAD":
                        if (downloadCommand(commandArgs, out))
                            out.println("Download Successful");
                        else
                            out.println("Invalid DOWNLOAD command: " + commandArgsString);
                        break;
                    case "LIST":
                        if (!listCommand(commandArgs, out))
                            out.println("Invalid LIST command: " + commandArgsString);
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

            try {
                FileManager.backup(filePath, out::println, isShareble);
                out.println("Backup successful");
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                out.println("Backup failed.");
            }

            return true;
        }

        private boolean downloadCommand(String[] commandArgs, PrintWriter out) {
            if (commandArgs.length != 1)    return false;

            final String filePath = commandArgs[0];
            if (!filePath.endsWith(".meta") && !filePath.endsWith(".own"))  return false;

            return FileManager.download(filePath, out::println);
        }

        private boolean listCommand(String[] commandArgs, PrintWriter out) {
            if (commandArgs.length > 1) return false;

            switch(commandArgs[0]) {
                case "--owner":
                case "-o":
                    out.print(OwnerStorage.listFiles());
                    return true;
                case "--chunks":
                case "-c":
                    out.print(ChunkStorage.listFiles());
                    return true;
                default:
                    return false;
            }
        }
    }
}
