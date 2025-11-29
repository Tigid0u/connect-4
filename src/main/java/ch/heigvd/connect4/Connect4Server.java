package ch.heigvd.connect4;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import ch.heigvd.connect4.utils.*;
import ch.heigvd.connect4.utils.Connect4.TurnResult;
import picocli.CommandLine;

import java.util.Random;

@CommandLine.Command(name = "server", version = "Connect4Server v1.0", mixinStandardHelpOptions = true)

/**
 * Connect4 server that handles multiple clients and manages game state
 */
public class Connect4Server implements Runnable{
    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Server's port. Default is ${DEFAULT-VALUE}",
            defaultValue = "4444"
    )
    private static int PORT;

    private static final int NB_THREADS = 2;
    private static final int ROWS = 4;
    private static final int COLUMNS = 4;

    private static final String END_OF_LINE = "\n";

    // Global game state
    private static boolean randomAlreadyDone = false;
    private static boolean newGame = true;
    private static boolean endOfGame = false;
    private static boolean opponentLeft = false;

    private static String userTurn;
    private static String opponentAction;

    private static Connect4 game = new Connect4(COLUMNS, ROWS);
    private static TurnResult turnResult = TurnResult.NOTHING;

    // Global server state
    private static final AtomicInteger nbClient = new AtomicInteger(0);
    private static final AtomicInteger nbOfReady = new AtomicInteger(0);
    private static final Set<String> userNames = ConcurrentHashMap.newKeySet();

    private static final Object mutex = new Object();

    private enum ClientState {
        JOIN,
        READY,
        IN_GAME
    }

    /**
     * Starts the Connect4 server
     */
    public void run() {
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

    /**
     * Resets the server game state if the previous game has ended
     */
    private static void resetServerGameStateIfNeeded() {
        if (!endOfGame && !opponentLeft) {
            return;
        }
        randomAlreadyDone = false;
        nbOfReady.set(0);
        turnResult = TurnResult.NOTHING;
        userTurn = null;
        newGame = true;
        opponentLeft = false;
    }

    /**
     * Handles a connected client
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientUserName;
        private String opponentUserName;
        private ClientState state = ClientState.JOIN;
        private final int id = nbClient.incrementAndGet();

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Runs the client handler to process client requests
         */
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
                logNewConnection();

                // Run REPL until client disconnects
                while (!socket.isClosed()) {
                    // When the client is in game, and it's not his turn, he will wait until it's his turn to play
                    // This is so the server can send the information to the client where his opponent played
                    handleTurnWaitingIfNeeded(bw);

                    // Server receives a request
                    String request = br.readLine();
                    // Client disconnected
                    if (request == null) {
                        socket.close();
                        continue;
                    } else if (opponentLeft) {
                        handleOpponentLeft(bw);
                    }

                    // Parse the request for the first command
                    ClientCommands command = parseCommand(request);
                    // Handle command
                    String response = handleCommand(command, request, bw);

                    bw.write(response + END_OF_LINE);
                    bw.flush();

                    // Check if this client win or draw
                    handleEndOfTurnForThisClient(bw);
                }

                // Cleanup on client disconnection
                handleClientDisconnection();
            } catch (IOException e) {
                System.err.println("[ERROR] exception: " + e);
            }
        }

        /**
         * Logs a new client connection
         */
        private void logNewConnection() {
            System.out.println("[Server] new client connected from " +
                    socket.getInetAddress().getHostAddress() +
                    ":" +
                    socket.getPort()
            );
        }

        /**
         * Handles waiting for the client's turn if needed. This method will block until it's the client's turn or the
         * game ends.
         * @param bw the BufferedWriter to send messages to the client
         * @throws IOException if an I/O error occurs
         */
        private void handleTurnWaitingIfNeeded(BufferedWriter bw) throws IOException {
            synchronized (mutex) {
                // If not in game or it's the client's turn, no need to wait
                if (state != ClientState.IN_GAME || userTurn == null || userTurn.equals(clientUserName)) {
                    return;
                }

                // Wait until it's the client's turn or the game ends
                while (!userTurn.equals(clientUserName) && !endOfGame && !opponentLeft) {
                    try {
                        mutex.wait();
                    } catch (InterruptedException e) {
                        System.err.println("Exception: " + e);
                        Thread.currentThread().interrupt();
                    }
                }

                // Game ended while waiting
                if (endOfGame) {
                    handleEndOfGameForWaitingClient(bw);
                    return;
                }

                // Opponent left while waiting
                if (opponentLeft) {
                    handleOpponentLeft(bw);
                    return;
                }

                // Now it's his turn, we send him the opponent's move
                bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                bw.flush();
            }
        }

        /**
         * Handles the end of the game for a waiting client
         * @param bw the BufferedWriter to send messages to the client
         * @throws IOException if an I/O error occurs
         */
        private void handleEndOfGameForWaitingClient(BufferedWriter bw) throws IOException {
            // Notify the client of the game result
            if (turnResult == TurnResult.WIN) {
                // Needs to send the opponent's last action before ending the game
                bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                bw.flush();
                // Then notify the client of the loss
                bw.write(ServerCommands.END_OF_GAME + " LOOSE" + END_OF_LINE);
            } else {
                // Needs to send the opponent's last action before ending the game
                bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                bw.flush();
                // Then notify the client of the draw
                bw.write(ServerCommands.END_OF_GAME + " DRAW" + END_OF_LINE);
            }
            bw.flush();

            cleanupUsername();
            resetServerGameStateIfNeeded();
            // Reset client state
            state = ClientState.JOIN;
        }

        /**
         * Handles the scenario where the opponent has left the game
         * @param bw the BufferedWriter to send messages to the client
         * @throws IOException if an I/O error occurs
         */
        private void handleOpponentLeft(BufferedWriter bw) throws IOException {
            state = ClientState.JOIN;
            resetServerGameStateIfNeeded();
            cleanupUsername();
            // Notify the client that the opponent has left and they win
            bw.write(ServerCommands.OPPONENT_LEFT + END_OF_LINE);
            bw.flush();
            bw.write(ServerCommands.END_OF_GAME + " WIN" + END_OF_LINE);
            bw.flush();
        }

        /**
         * Parses the client command from the request string
         * @param request the request string from the client
         * @return the parsed ClientCommands enum, or null if unknown
         */
        private ClientCommands parseCommand(String request) {
            String[] requestParsed = request.split(" ", 2);
            try {
                return ClientCommands.valueOf(requestParsed[0]);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Handles the client command and returns the appropriate response
         * @param command the client command to handle
         * @param request the original request string
         * @param bw the BufferedWriter to send messages to the client
         * @return the response string to send back to the client
         * @throws IOException if an I/O error occurs
         */
        private String handleCommand(ClientCommands command, String request, BufferedWriter bw) throws IOException {
            return switch (command) {
                case JOIN -> handleJoin(request);
                case READY -> handleReady();
                case PLAY -> handlePlay(request);
                case null, default -> ServerCommands.ERROR + " unknown_message";
            };
        }

        /**
         * Handles the JOIN command from the client
         * @param request the request string from the client
         * @return the response string to send back to the client
         */
        private String handleJoin(String request) {
            if (state != ClientState.JOIN) {
                return ServerCommands.ERROR + " invalid_order";
            }

            // Parse username and check presence
            String[] parts = request.split(" ", 2);
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                return ServerCommands.ERROR + " missing_username";
            }

            // Taking the username from the request
            String[] arguments = parts[1].split(" ", 2);
            String requestedUserName = arguments[0];

            synchronized (mutex) {
                // Check if the username is already being used thanks to the global set that returns false if the
                // username is already present
                if (!userNames.add(requestedUserName)) {
                    return ServerCommands.ERROR + " username_used";
                }
                // Register the username for this client
                clientUserName = requestedUserName;
            }

            // Update the state the client has to be for the next iteration
            state = ClientState.READY;
            return String.valueOf(ServerCommands.OK);
        }

        /**
         * Handles the READY command from the client
         * @return the response string to send back to the client
         */
        private String handleReady() {
            if (state != ClientState.READY) {
                return ServerCommands.ERROR + " invalid_order";
            }

            synchronized (mutex) {
                nbOfReady.incrementAndGet();
                // Notify other threads that a player is ready
                mutex.notifyAll();

                // Wait until all players are ready
                while (nbOfReady.get() != NB_THREADS) {
                    try {
                        mutex.wait();
                    } catch (InterruptedException e) {
                        System.err.println("Exception: " + e);
                        Thread.currentThread().interrupt();
                    }
                }

                // Choose starting player and opponent if not already done
                chooseStartingPlayerAndOpponent();
                initGameIfNeeded();
                // Reset Game state for new game
                endOfGame = false;
            }

            // Update the state the client has to be for the next iteration of the while() loop
            state = ClientState.IN_GAME;
            return ServerCommands.GAME_STARTS + " " + opponentUserName + " " +
                    (userTurn.equals(clientUserName) ? "1" : "0");
        }

        /**
         * Chooses the starting player and opponent for the game
         */
        private void chooseStartingPlayerAndOpponent() {
            // Choose starting player randomly if not already done
            if (!randomAlreadyDone) {
                int starterIndex = new Random().nextInt(NB_THREADS);
                int i = 0;
                for (String name : userNames) {
                    if (i == starterIndex) {
                        userTurn = name;
                        randomAlreadyDone = true;
                    }
                    i++;
                }
            }

            // Determine opponent's username
            for (String name : userNames) {
                if (!name.equals(clientUserName)) {
                    opponentUserName = name;
                    break;
                }
            }
        }

        /**
         * Initializes a new game if needed
         */
        private void initGameIfNeeded() {
            if (newGame) {
                newGame = false;
                game = new Connect4(COLUMNS, ROWS);
            }
        }

        /**
         * Handles the PLAY command from the client
         * @param request the request string from the client
         * @return the response string to send back to the client
         */
        private String handlePlay(String request) {
            if (state != ClientState.IN_GAME) {
                return ServerCommands.ERROR + " invalid_order";
            }

            synchronized (mutex) {
                if (!clientUserName.equals(userTurn)) {
                    return ServerCommands.ERROR + " not_your_turn";
                }
            }

            // Parse request and check format
            String[] requestParsed = request.split(" ", 2);
            if (requestParsed.length < 2 || requestParsed[1].trim().isEmpty()) {
                return ServerCommands.ERROR + " invalid_format";
            }

            // Taking the column played from the request and check for extra arguments
            String[] arguments = requestParsed[1].split(" ", 2);
            if (arguments.length > 1 && !arguments[1].trim().isEmpty()) {
                return ServerCommands.ERROR + " invalid_format";
            }

            String columnPlayed = arguments[0];

            synchronized (mutex) {
                try {
                    // Playing the game
                    turnResult = game.play(Integer.parseInt(columnPlayed), id);
                } catch (IllegalArgumentException e) {
                    return ServerCommands.ERROR + " invalid_input";
                }

                // Store the opponent's action to notify them later
                opponentAction = columnPlayed;

                // Notify opponent if the game is not over
                if (turnResult != TurnResult.WIN && turnResult != TurnResult.DRAW) {
                    mutex.notifyAll();
                }

                // Give the opponent the rights to play
                userTurn = opponentUserName;
            }

            return String.valueOf(ServerCommands.OK);
        }

        /**
         * Handles the end of turn for this client, checking for win or draw conditions
         * @param bw the BufferedWriter to send messages to the client
         * @throws IOException if an I/O error occurs
         */
        private void handleEndOfTurnForThisClient(BufferedWriter bw) throws IOException {
            synchronized (mutex) {
                if (turnResult == TurnResult.WIN) {
                    handleEndOfGameAsWinner(bw, "WIN");
                } else if (turnResult == TurnResult.DRAW) {
                    handleEndOfGameAsWinner(bw, "DRAW");
                }
            }
        }

        /**
         * Handles the end of the game when this client is the winner or the game is a draw
         * @param bw the BufferedWriter to send messages to the client
         * @param result the result of the game ("WIN" or "DRAW")
         * @throws IOException if an I/O error occurs
         */
        private void handleEndOfGameAsWinner(BufferedWriter bw, String result) throws IOException {
            cleanupUsername();
            state = ClientState.JOIN;
            endOfGame = true;
            mutex.notifyAll();

            bw.write(ServerCommands.END_OF_GAME + " " + result + END_OF_LINE);
            bw.flush();
        }

        /**
         * Handles client disconnection and performs necessary cleanup
         */
        private void handleClientDisconnection() {
            synchronized (mutex) {
                // Notify opponent if the game was ongoing and the opponent left
                if (!endOfGame && (state == ClientState.IN_GAME)) {
                    opponentLeft = true;
                    mutex.notifyAll();
                }

                nbClient.decrementAndGet();

                System.out.println("[Server] client " + clientUserName + " disconnected\n" +
                        "[Server] closing connection");
                cleanupUsername();
            }
        }

        /**
         * Cleans up the client's username from the global set
         */
        private void cleanupUsername() {
            if (clientUserName != null) {
                userNames.remove(clientUserName);
            }
        }

        /*
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
                    synchronized (mutex) {
                        // When the client is in game, and it's not his turn, he will wait until it's his turn to play
                        // This is so the server can send the information to the client where his opponent played
                        if ((state == ClientState.IN_GAME) && (!userTurn.equals(clientUserName))) {
                            while (!userTurn.equals(clientUserName) && (!endOfGame)) {
                                try {
                                    mutex.wait();
                                } catch (InterruptedException e) {
                                    System.err.println("Exception: " + e);
                                    Thread.currentThread().interrupt();
                                }
                            }
                            if (endOfGame) {
                                if (turnResult ==  TurnResult.WIN) {
                                    bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                                    bw.flush();
                                    bw.write(ServerCommands.END_OF_GAME + " LOOSE" + END_OF_LINE);
                                } else {
                                    bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                                    bw.flush();
                                    bw.write(ServerCommands.END_OF_GAME + " DRAW" + END_OF_LINE);
                                }
                                // Removes the client username from the server's data
                                if (clientUserName != null) {
                                    userNames.remove(clientUserName);
                                }
                                resetServerAttributes();
                                state = ClientState.JOIN;
                            } else {
                                if (opponentLeft) {
                                    state = ClientState.JOIN;
                                    resetServerAttributes();
                                    // Removes the client username from the server's data
                                    if (clientUserName != null) {
                                        userNames.remove(clientUserName);
                                    }
                                    bw.write(ServerCommands.END_OF_GAME + " WIN" + END_OF_LINE);
                                } else {
                                    bw.write(ServerCommands.YOUR_TURN + " " + opponentAction + END_OF_LINE);
                                }
                            }
                            bw.flush();
                        }
                    }
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

                            synchronized (mutex) {
                                // Check if the username is already being used
                                if (!userNames.add(clientUserName)) {
                                    clientUserName = null;
                                    yield ServerCommands.ERROR + " username_used";
                                }
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

                            // Wait for all the players to be ready
                            synchronized (mutex) {
                                nbOfReady.incrementAndGet();
                                mutex.notifyAll();

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

                                if (newGame) {
                                    newGame = false;
                                    game = new Connect4(COLUMNS, ROWS);
                                }
                                // Reset Game state
                                endOfGame = false;
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

                            synchronized (mutex) {
                                // Check if it's the client's turn to play
                                if (!userTurn.equals(clientUserName)) {
                                    yield ServerCommands.ERROR + " not_your_turn";
                                }
                            }

                            String[] arguments = requestParsed[1].split(" ", 2);
                            // Check if there is another arguments apart from the column the player wants to play
                            // If there is a second argument, but it's a " " then it's not a problem
                            if ((arguments.length > 1) && !(arguments[1].trim().isEmpty())) {
                                yield ServerCommands.ERROR + " invalid_format";
                            }
                            String columnPlayed = arguments[0];

                            synchronized (mutex) {
                                // Playing the game
                                try {
                                    turnResult = game.play(Integer.parseInt(columnPlayed), id);
                                } catch (IllegalArgumentException e) {
                                    yield ServerCommands.ERROR + " invalid_input";
                                }
                                opponentAction = columnPlayed;

                                if ((turnResult != TurnResult.WIN) && (turnResult != TurnResult.DRAW)) {
                                    mutex.notifyAll();
                                }
                                // Give the opponent the rights to play
                                userTurn = opponentUserName;
                            }
                            yield String.valueOf(ServerCommands.OK);

                        }
                        case null, default -> {
                            yield ServerCommands.ERROR + " unknown_message";
                        }
                    };

                    // Send the result of the request
                    bw.write(response + END_OF_LINE);
                    bw.flush();

                    // Check if this client win
                    synchronized (mutex) {
                        // Check if this client win
                        if (turnResult == TurnResult.WIN) {
                            // Removes the client username from the server's data
                            if (clientUserName != null) {
                                userNames.remove(clientUserName);
                            }
                            state = ClientState.JOIN;
                            endOfGame = true;
                            mutex.notifyAll();
                            bw.write(ServerCommands.END_OF_GAME + " WIN" + END_OF_LINE);
                            bw.flush();
                        } else if (turnResult == TurnResult.DRAW) {
                            // Removes the client username from the server's data
                            if (clientUserName != null) {
                                userNames.remove(clientUserName);
                            }
                            state = ClientState.JOIN;
                            endOfGame = true;
                            mutex.notifyAll();
                            bw.write(ServerCommands.END_OF_GAME + " DRAW" + END_OF_LINE);
                            bw.flush();
                        }
                    }

                }
                synchronized (mutex) {
                    if (!endOfGame && (state == ClientState.IN_GAME)) {
                        opponentLeft = true;
                        mutex.notifyAll();
                    }

                    // Removes client
                    nbClient.decrementAndGet();
                    System.out.println("Nb of client: " + nbClient);
                    // Removes the client username from the server's data
                    if (clientUserName != null) {
                        userNames.remove(clientUserName);
                    }
                }

                System.out.println("[SERVER] client " + clientUserName + " disconnected\n" +
                        "[SERVER] closing connection");
            } catch (IOException e) {
                System.err.println("[ERROR] exception: " + e);
            }
        }*/
    }
}
