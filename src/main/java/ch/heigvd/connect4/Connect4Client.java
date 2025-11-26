package ch.heigvd.connect4;

import ch.heigvd.connect4.utils.*;
import ch.heigvd.connect4.utils.ClientCommands;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "client", version = "Connect4Client v1.0", mixinStandardHelpOptions = true)

public class Connect4Client implements Runnable {
  public static String END_OF_LINE = "\n";
  private String username;

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
        username = CliClientUtils.getUserInput("Please type in your username");

        // Join the server
        try {
          retval = joinRequest(in, out);
        } catch (IOException e) {
          System.out.println("[Client] JOIN request failed: " + e.getMessage());
          return;
        }

        // If join request is denied, we start over (ask username and join the server)
        if (retval != ReturnValue.OK) {
          continue;
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
  private ReturnValue joinRequest(BufferedReader in, BufferedWriter out) throws IOException {
    String request = null, response = null;
    String[] responseParts = null;

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

    if (responseParts[0] == "ERROR") {
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
          System.out.println("Unknown response from the server !");
          return ReturnValue.UNKNOWN_RESPONSE;
        }
      }
    } else if (responseParts[0] == "OK") {
      return ReturnValue.OK;
    } else {
      return ReturnValue.UNKNOWN_RESPONSE;
    }
  }
}
