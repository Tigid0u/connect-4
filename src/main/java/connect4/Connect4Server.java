package connect4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import connect4.utils.*;

public class Connect4Server {
    private static final int PORT = 4444;
    private static final int NB_THREADS = 2;

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT);
            ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS)) {
            System.out.println("[Server] listening on port: " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = new serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("[ERROR] exception: " + e);
        }
    }
}
