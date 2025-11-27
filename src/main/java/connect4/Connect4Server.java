package connect4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import connect4.utils.*;
import java.util.Random;

public class Connect4Server {
    private static final int PORT = 4444;
    private static final int NB_THREADS = 2;
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;
    private static int opponentAction;

    private static final String END_OF_LINE = "\n";
    private static String userTurn;

    private static final Connect4 game = new Connect4(COLUMNS, ROWS);

    // Atomic attributes
    private static final AtomicInteger nbClient = new AtomicInteger(0);
    private static final AtomicInteger nbOfReady = new AtomicInteger(0);
    // ChatGPT gave me the equivalent of a Set but for concurrency
    private static final Set<String> userNames = ConcurrentHashMap.newKeySet();

    private static final Object mutex = new Object();

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
        private String opponentUserName;
        private ClientState state = ClientState.JOIN;

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
                    // Server receives a request

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
                            // Check order condition
                            if (state != ClientState.JOIN) {
                                yield ServerCommands.ERROR + " invalid_order";
                            }

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

                            // Update the state the client has to be for the next iteration
                            state = ClientState.READY;

                            // The client was registered to play connect 4
                            yield ServerCommands.OK + "";
                        }
                        case READY -> {
                            // Check order condition
                            if (state != ClientState.READY) {
                                yield ServerCommands.ERROR + " invalid_order";
                            }

                            nbOfReady.incrementAndGet();
                            mutex.notifyAll();

                            // Wait for all the players to be ready
                            synchronized (mutex) {
                                while (nbOfReady.get() != NB_THREADS) {
                                    try {
                                        mutex.wait();
                                    } catch (InterruptedException e) {
                                        System.err.println("Exception: " + e);
                                        Thread.currentThread().interrupt();
                                    }
                                }
                                // Decides who starts
                                Random rand = new Random();
                                Iterator<String> it = userNames.iterator();
                                int userIndex = rand.nextInt(NB_THREADS);

                                // This'll give us the opponent's name and who starts randomly
                                int i = 0;
                                while (it.hasNext()) {
                                    if (i <= userIndex) {
                                        userTurn = it.next();
                                    }
                                    if (!clientUserName.equals(it.toString())) {
                                        opponentUserName = it.toString();
                                    }
                                    ++i;
                                }
                            }
                            // Update the state the client has to be for the next iteration of the while() loop
                            state = ClientState.IN_GAME;

                            yield ServerCommands.GAME_STARTS + " " + opponentUserName +
                                    (userTurn.equals(clientUserName) ? " 1" : " 0");
                        }
                        case PLAY -> {
                            // Check order condition
                            if (state != ClientState.IN_GAME) {
                                yield ServerCommands.ERROR + " invalid_order";
                            }

                            // Check if it's the client's turn to play
                            if (!userTurn.equals(clientUserName)) {
                                yield ServerCommands.ERROR + " not_your_turn";
                            }

                            yield "";
                        }
                        case null, default -> {
                            yield ServerCommands.ERROR + " unknown_message";
                        }
                    };

                    // Making this verification in case we don't need to send anything to the client
                    if (!response.isEmpty()) {
                        // Send the result of the request
                        bw.write(response + END_OF_LINE);
                        bw.flush();
                    }
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
