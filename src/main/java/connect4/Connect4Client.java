package connect4;

import connect4.utils.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

@Command(version = "Connect4Client v1.0", mixinStandardHelpOptions = true)

public class Connect4Client implements Runnable {
  public static String END_OF_LINE = "\n";
  private String username;

  @Option(names = { "-h",
      "--hostname" }, description = "Hostname of the server. Default is ${DEFAULT-VALUE}", defaultValue = "localhost")
  private String hostname;

  @Option(names = { "-p",
      "--port" }, description = "Port of the server. Default is ${DEFAULT-VALUE}", defaultValue = "4444")
  private int port;

  public void run() {
    System.out.println("[Client] Connecting to " + hostname + ":" + port + "...");
    try (Socket socket = new Socket(hostname, port);
        Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(reader);
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter out = new BufferedWriter(writer);
        Reader systemInReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader bsir = new BufferedReader(systemInReader)) {

      CliClientUtils.displayBanner();

      while (!socket.isClosed()) {
        // TODO: Get user input
        username = CliClientUtils.getUserInput("Please type in your username");

      }

    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Connect4Client()).execute(args);
    System.exit(exitCode);
  }
}
