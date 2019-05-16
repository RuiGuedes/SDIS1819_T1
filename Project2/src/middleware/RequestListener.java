package middleware;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
                out.println("Hello there old chum.");
                System.out.println(in.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
