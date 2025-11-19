package connect4;

import connect4.utils.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Connect4Client {
  private static final String HOST = "localhost";
  private static final int PORT = 1234;
  public static String END_OF_LINE = "\n";

  public static void main(String[] args) {
    System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");
    try (Socket socket = new Socket(HOST, PORT);
        Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(reader);
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter out = new BufferedWriter(writer);
        Reader systemInReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader bsir = new BufferedReader(systemInReader)) {

      // TODO: show game menu

      while (!socket.isClosed()) {
        // TODO: to complete
      }

    } catch (Exception e) {
      // TODO: handle errors
    }
  }
}
