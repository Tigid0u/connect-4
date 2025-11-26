package connect4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import connect4.utils.*;

public class Connect4Server {
    private static final int PORT = 4444;
    private static final int NB_THREADS = 2;
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;
    private static final String END_OF_LINE = "\n";

    private static final Connect4 game = new Connect4(COLUMNS, ROWS);

    // Atomic attributes
    private static final AtomicInteger nbClient = new AtomicInteger(0);
    private static final AtomicInteger nbOfReady = new AtomicInteger(0);
    // ChatGPT gave me the equivalent of a Set but for concurrency
    private static final Set<String> userNames = ConcurrentHashMap.newKeySet();

    private enum ClientState {
        JOIN,
        READY,
        IN_GAME
    }

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
        private String clientUserName;
        private boolean ready;
        private ClientState state = ClientState.JOIN;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            ready = false;
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
                    /*-- Server receives a request --*/

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

                    // Convert String to ClientCommands
                    ClientCommands command = null;
                    try {
                        command = ClientCommands.valueOf(requestParsed[0]);
                    } catch (Exception e) {
                        // Do nothing
                    }

                    // Handle command
                    String response = null;
                    response = switch (command) {
                        case JOIN -> {
                            //

                            // Check if there is a userName
                            if (requestParsed.length < 2 || requestParsed[1].trim().isEmpty()) {
                                yield ServerCommands.ERROR + " missing_username";
                            }

                            // Taking the username from the request
                            String[] arguments = requestParsed[1].split(" ", 2);
                            clientUserName = arguments[0];

                            // Check if the username is already being used
                            if (!userNames.add(clientUserName)) {
                                yield ServerCommands.ERROR + " username_used";
                            }

                            // The client was registered to play connect 4
                            yield ServerCommands.OK + "";
                        }
                        case READY -> {
                            nbOfReady.incrementAndGet();
                            ready = true;
                            yield "";
                        }
                        case PLAY -> {

                        }
                        case null, default -> {
                            yield ServerCommands.ERROR + " unknown_message";
                        }
                    };

                    // Send the result of the request
                    bw.write(response + END_OF_LINE);
                    bw.flush();

                    // Ready to play
                    if (ready) {

                        while (!socket.isClosed() || ) {

                        }
                    }

                    /*-- Server makes a request --*/
                }
                // Removes client
                nbClient.decrementAndGet();
                // Removes the client username from the server's data
                if (clientUserName != null) {
                    userNames.remove(clientUserName);
                }

                System.out.println("[SERVER] client " + clientUserName + " disconnected\n" +
                        "[SERVER] closing connection");
            } catch (IOException e) {
                System.err.println("[ERROR] exception: " + e);
            }
        }
    }
}
