package ch.heigvd.connect4;

import ch.heigvd.connect4.utils.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "client", version = "Connect4Client v1.0", mixinStandardHelpOptions = true)

public class Connect4Client implements Runnable {
  public static String END_OF_LINE = "\n";
  private String username, opponentUsername;
  private boolean isMyTurn;
  private static final int WIDTH = 4;
  private static final int HEIGHT = 4;

  private enum ReturnValue {
    OK,
    UNKNOWN_RESPONSE,
    USERNAME_USED,
    SERVER_FULL
  }

  @Option(names = { "-o",
      "--hostname" }, description = "Hostname of the server. Default is ${DEFAULT-VALUE}", defaultValue = "localhost")
  private String hostname;

  @Option(names = { "-p",
      "--port" }, description = "Port of the server. Default is ${DEFAULT-VALUE}", defaultValue = "4444")
  private int port;

  public void run() {
    ReturnValue retval;
    System.out.println("[Client] Connecting to " + hostname + ":" + port + "...");
    try (Socket socket = new Socket(hostname, port);
        Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(reader);
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter out = new BufferedWriter(writer);
        Reader systemInReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader bsir = new BufferedReader(systemInReader)) {

      CliClientUtils.clearScreen();
      CliClientUtils.displayBanner();

      while (!socket.isClosed()) {
        // Join the server
        try {
          retval = joinRequest(in, out);
        } catch (Exception e) {
          System.out.println("[Client] JOIN request failed: " + e.getMessage());
          return;
        }

        // If join request is denied, we start over (ask username and join the server)
        if (retval != ReturnValue.OK) {
          continue;
        }

        // Send READY request to the server
        try {
          sendReadyRequest(out);
        } catch (IOException e) {
          System.out.println("[Client] READY request failed: " + e.getMessage());
          continue;
        } catch (IllegalArgumentException e) {
          System.out.println("[Client] Invalid READY request: " + e.getMessage());
          continue;
        }

        // Wait for the GAME_STARTS notification from the server
        try {
          waitForGameStartNotification(in);
        } catch (IOException e) {
          System.out.println("[Client] Waiting for GAME_STARTS notification failed: " + e.getMessage());
          continue;
        } catch (IllegalArgumentException e) {
          System.out.println("[Client] Invalid GAME_STARTS notification: " + e.getMessage());
          continue;
        }

        // If we reach this point, we are successfully connected and ready to play
        System.out.println("Game is starting against opponent: " + opponentUsername);

        Connect4 game = new Connect4(WIDTH, HEIGHT);
        Connect4.TurnResult turnResult = Connect4.TurnResult.NOTHING;

        while (turnResult != Connect4.TurnResult.WIN && turnResult != Connect4.TurnResult.DRAW) {
          CliClientUtils.clearScreen();

          // Print board
          CliClientUtils.printBoard(game.getGrid());

          if (isMyTurn) {
            int column;
            while (true) {
              column = CliClientUtils.getUserInputInt(
                  "It's your turn! Please enter the column number (0-" + (WIDTH - 1) + ") to drop your disc:", 0,
                  WIDTH - 1);
              if (game.checkInput(column)) {
                break;
              } else {
                System.out.println("This column is full. Please choose another column");
              }
            }
            turnResult = game.play(column, 1);

            // Send PLAY request to the server
            try {
              sendPlayRequest(out, in, column);
            } catch (Exception e) {
              System.out.println("[Client] PLAY request failed: " + e.getMessage());
              // Should not happen in normal conditions, so if it does, we exit
              return;
            }

            isMyTurn = false;
          } else {
            System.out.println("Waiting for opponent's move...");
            int opponentColumn;
            try {
              opponentColumn = receiveOpponentPlay(in);
            } catch (Exception e) {
              System.out.println("[Client] Receiving opponent's play failed: " + e.getMessage());
              // Should not happen in normal conditions, so if it does, we exit
              return;
            }
            turnResult = game.play(opponentColumn, 2);
            isMyTurn = true;
          }
        }
          CliClientUtils.clearScreen();
          // Print board a last time to see the final result
          CliClientUtils.printBoard(game.getGrid());
        try {
          receiveGameResult(in);
        } catch (Exception e) {
          System.out.println("[Client] Receiving game result failed: " + e.getMessage());
        }
      }

    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e.getMessage());
    }
  }

  /**
   * Sends a JOIN request to the server
   *
   * @param in  a buffered input stream from the socket
   * @param out a buffered output stream from the socket
   *
   * @return the status of completion of the request
   **/
  private ReturnValue joinRequest(BufferedReader in, BufferedWriter out) throws IOException, IllegalArgumentException {
    String request = null, response = null;
    String[] responseParts = null;
      username = CliClientUtils.getUserInput("Please type in your username");

    // Request to join server using JOIN command.
    request = ClientCommands.JOIN + " " + username + END_OF_LINE;

    // Send request to server
    out.write(request);
    out.flush();

    // Read server's response
    response = in.readLine();

    if (response == null) {
      throw new IOException("Null response from the server");
    }

    responseParts = response.split(" ", 2);

    ServerCommands serverCommand = ServerCommands.valueOf(responseParts[0]);

    if (serverCommand == ServerCommands.ERROR) {
      switch (responseParts[1]) {
        case "username_used" -> {
          System.out.println("This username is already used. Please choose another username !");
          return ReturnValue.USERNAME_USED;
        }
        case "server_full" -> {
          System.out.println("The server is full. Please try again later !");
          return ReturnValue.SERVER_FULL;
        }
        case null, default -> {
          throw new IllegalArgumentException("Unknown ERROR parameter: " + responseParts[1]);
        }
      }
    } else if (serverCommand == ServerCommands.OK) {
      return ReturnValue.OK;
    } else {
      throw new IllegalArgumentException("Unknown response from server: " + response);
    }
  }

  /**
   * Sends a READY request to the server
   *
   * @param out a buffered output stream from the socket
   *
   * @throws IOException if sending the request fails
   **/
  private void sendReadyRequest(BufferedWriter out) throws IOException {
    String request = ClientCommands.READY + " " + END_OF_LINE;

    // Send request to server
    out.write(request);
    out.flush();

    // As no response is expected, we do not read anything from the server
  }

  /**
   * Waits for a GAME_STARTS notification from the server
   *
   * @param in a buffered input stream from the socket
   *
   * @throws IOException              if reading the notification fails
   * @throws IllegalArgumentException if the notification is invalid
   **/
  private void waitForGameStartNotification(BufferedReader in) throws IOException, IllegalArgumentException {
    String response = null;
    String[] responseParts = null;

    // Read server's response
    response = in.readLine();

    if (response == null) {
      throw new IOException("Null response from the server");
    }

    responseParts = response.split(" ", 3);
    // Convert response to ServerCommand
    ServerCommands gameStartCommand = ServerCommands.valueOf(responseParts[0]);

    if (gameStartCommand != ServerCommands.GAME_STARTS) {
      throw new IllegalArgumentException("Expected GAME_STARTS notification, but received: " + response);
    } else if (responseParts.length < 3) {
      throw new IllegalArgumentException("GAME_STARTS notification missing parameters: " + response);
    }

    // Opponnent username is in the second part of the response
    this.opponentUsername = responseParts[1];

    // Is my turn info is in the third part of the response
    this.isMyTurn = responseParts[2].equals("1");
  }

  /**
   * Sends a PLAY request to the server
   *
   * @param out    a buffered output stream from the socket
   * @param in     a buffered input stream from the socket
   * @param column the column where to drop the disc
   *
   * @throws IOException if sending the request fails or if the server returns an
   *                     error
   **/
  private void sendPlayRequest(BufferedWriter out, BufferedReader in, int column) throws IOException {
    String request = ClientCommands.PLAY + " " + column + END_OF_LINE;

    // Send request to server
    out.write(request);
    out.flush();

    // Get server's response
    String response = in.readLine();

    if (response == null) {
      throw new IOException("Null response from the server");
    }

    String[] responseParts = response.split(" ", 2);
    // Convert response to ServerCommand
    ServerCommands serverCommand = ServerCommands.valueOf(responseParts[0]);

    if (serverCommand == ServerCommands.OK) {
      return;
    } else if (serverCommand == ServerCommands.ERROR) {
      if (responseParts.length < 2) {
        throw new IOException("ERROR response missing parameters: " + response);
      }
      throw new IOException("Server returned ERROR: " + responseParts[1]);
    } else {
      throw new IOException("Unknown response from server: " + response);
    }
  }

  /**
   * Receives the opponent's play from the server
   *
   * @param in a buffered input stream from the socket
   *
   * @return the column where the opponent dropped their disc
   *
   * @throws IOException if reading the notification or the opponnent's column is
   *                     invalid
   **/
  private int receiveOpponentPlay(BufferedReader in) throws IOException, IllegalArgumentException {
    // Wait for opponent's play (YOUR_TURN notification)
    String response = in.readLine();
    int opponentColumn;

    if (response == null) {
      throw new IOException("Null response from the server");
    }

    String[] responseParts = response.split(" ", 2);
    // Convert response to ServerCommand
    ServerCommands serverCommand = ServerCommands.valueOf(responseParts[0]);

    if (serverCommand != ServerCommands.YOUR_TURN) {
      throw new IOException("Expected YOUR_TURN notification, but received: " + response);
    } else if (responseParts.length < 2) {
      throw new IOException("YOUR_TURN notification missing parameters: " + response);
    }

    try {
      opponentColumn = Integer.parseInt(responseParts[1]);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid column number in YOUR_TURN notification: " + responseParts[1]);
    }

    if (opponentColumn < 0 || opponentColumn >= WIDTH) {
      throw new IOException("Column number out of range in YOUR_TURN notification: " + opponentColumn);
    }

    return opponentColumn;
  }

  /**
   * Receives the game result from the server
   *
   * @param in a buffered input stream from the socket
   *
   * @throws IOException              if reading the notification fails
   * @throws IllegalArgumentException if the notification is invalid
   **/
  private void receiveGameResult(BufferedReader in) throws IOException, IllegalArgumentException {
    String response = in.readLine();

    if (response == null) {
      throw new IOException("Null response from the server");
    }

    String[] responseParts = response.split(" ", 2);
    // Convert response to ServerCommand
    ServerCommands serverCommand = ServerCommands.valueOf(responseParts[0]);

    if (serverCommand != ServerCommands.END_OF_GAME) {
      new IOException("Expected END_OF_GAME notification, but received: " + response);
    } else if (responseParts.length < 2) {
      throw new IOException("END_OF_GAME notification missing parameters: " + response);
    }

    switch (responseParts[1]) {
      case "WIN" -> System.out.println("You won!");
      case "LOOSE" -> System.out.println("You lost!");
      case "DRAW" -> System.out.println("It's a draw!");
      default -> throw new IllegalArgumentException("Invalid END_OF_GAME parameter: " + responseParts[1]);
    }
  }
}
