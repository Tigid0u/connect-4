package connect4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import connect4.utils.*;

public class Connect4Server {
    private static final int PORT = 4444;
    private static final int NB_THREADS = 2;
    private static final String END_OF_LINE = "\n";

    // Atomic attributes
    private static final AtomicInteger nbClient = new AtomicInteger(0);
    private static final Map<String, Integer> clientReady = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT);
            ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS)
        ) {
            System.out.println("[Server] listening on port: " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] exception: " + e);
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    socket;
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                    );
                    BufferedWriter bw = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                    )
            ) {
                System.out.println("[SERVER] new client connected from " +
                        socket.getInetAddress().getHostAddress() +
                        ":" +
                        socket.getPort()
                );

                // Run REPL until client disconnects
                while (!socket.isClosed()) {
                    String request = br.readLine();

                    // Client disconnected
                    if (request == null) {
                        socket.close();
                        continue;
                    }

                    // Increment number of clients on the server
                    nbClient.incrementAndGet();

                    // Parse the request for the first command
                    String[] requestParsed = request.split(" ", 2);

                    ClientCommands command = null;
                    try {
                        command = ClientCommands.valueOf(requestParsed[0]);
                    } catch (Exception e) {
                        // Do nothing
                    }

                    // Prepare response
                    String response = null;
                    switch (command) {
                        case JOIN -> {
                            String[] arguments = requestParsed[1].split(" ", 2);
                            String userName = arguments[0];


                        }
                        case READY -> {

                        }
                        case PLAY -> {

                        }
                        case null, default -> {

                        }
                    }

                    // Send the result of the request
                    bw.write(response + END_OF_LINE);
                    bw.flush();
                }
            } catch (IOException e) {
                System.err.println("[ERROR] exception: " + e);
            }
        }
    }
}
