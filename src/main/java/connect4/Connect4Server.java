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
import connect4.utils.Connect4.TurnResult;
import java.util.Random;

public class Connect4Server {
    private static final int PORT = 4444;
    private static final int NB_THREADS = 2;
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;

    private static boolean randomAlreadyDone = false;
    private static boolean gameCreated = false;
    private static boolean endOfGame = false;

    private static final String END_OF_LINE = "\n";
    private static String userTurn;
    private static String opponentAction;

    private static Connect4 game = new Connect4(COLUMNS, ROWS);

    // Atomic attributes
    private static final AtomicInteger nbClient = new AtomicInteger(0);
    private static final AtomicInteger nbOfReady = new AtomicInteger(0);
    // ChatGPT gave me the equivalent of a Set but for concurrency
    private static final Set<String> userNames = ConcurrentHashMap.newKeySet();

    private static final Object mutex = new Object();

    private static TurnResult turnResult;

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
        private final int id = nbClient.incrementAndGet();

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
                            yield String.valueOf(ServerCommands.OK);
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

                                // Decides who starts. Index of the starter is somewhere in {0, ... , NB_THREADS - 1}
                                Random rand = new Random();
                                int userIndex = rand.nextInt(NB_THREADS);

                                // This'll give us the opponent's name and who starts randomly if it was not done
                                // already by the other client
                                int i = 0;
                                for (String name : userNames) {
                                    // Get the name of the starting player
                                    if ((!randomAlreadyDone) && (i == userIndex)) {
                                        userTurn = name;
                                        randomAlreadyDone = true;
                                    }
                                    // Get the name of the opponent
                                    if (!clientUserName.equals(name)) {
                                        opponentUserName = name;
                                    }
                                    ++i;
                                }

                                if (!gameCreated) {
                                    gameCreated = true;
                                    game = new Connect4(COLUMNS, ROWS);
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

                            String[] arguments = requestParsed[1].split(" ", 2);
                            // Check if there is another arguments apart from the column the player wants to play
                            // If there is a second argument, but it's a " " then it's not a problem
                            if ((arguments.length > 1) && !(arguments[1].trim().isEmpty())) {
                                yield ServerCommands.ERROR + " invalid_format";
                            }
                            String columnPlayed = arguments[0];

                            // Playing the game
                            try {
                                turnResult = game.play(Integer.parseInt(columnPlayed), id);
                            } catch (IllegalArgumentException e) {
                                yield ServerCommands.ERROR + " invalid_input";
                            }
                            opponentAction = columnPlayed;

                            // Result of the turn
                            if (turnResult == TurnResult.WIN) {
                                endOfGame = true;
                                yield ServerCommands.END_OF_GAME + " WIN";
                            } else if (turnResult == TurnResult.DRAW) {
                                endOfGame = true;
                                yield ServerCommands.END_OF_GAME + " DRAW";
                            } else {
                                userTurn = opponentUserName;
                                yield String.valueOf(ServerCommands.OK);
                            }
                        }
                        case null, default -> {
                            yield ServerCommands.ERROR + " unknown_message";
                        }
                    };

                    if (endOfGame) {
                        randomAlreadyDone = false;
                    }

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
