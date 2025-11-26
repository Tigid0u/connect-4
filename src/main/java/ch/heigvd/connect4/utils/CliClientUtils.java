package ch.heigvd.connect4.utils;

import java.util.Scanner;

public class CliClientUtils {
  private static final Scanner sin = new Scanner(System.in);

  /**
   * Displays connect 4 logo as a banner
   **/
  public static void displayBanner() {
    String header = """
        ╔════════════════════════════════════════════════════════════════════════════════════════════╗
        ║                                                                                            ║
        ║     █████████                                                    █████       █████ █████   ║
        ║    ███░░░░░███                                                  ░░███       ░░███ ░░███    ║
        ║   ███     ░░░   ██████  ████████   ████████    ██████   ██████  ███████      ░███  ░███ █  ║
        ║  ░███          ███░░███░░███░░███ ░░███░░███  ███░░███ ███░░███░░░███░       ░███████████  ║
        ║  ░███         ░███ ░███ ░███ ░███  ░███ ░███ ░███████ ░███ ░░░   ░███        ░░░░░░░███░█  ║
        ║  ░░███     ███░███ ░███ ░███ ░███  ░███ ░███ ░███░░░  ░███  ███  ░███ ███          ░███░   ║
        ║   ░░█████████ ░░██████  ████ █████ ████ █████░░██████ ░░██████   ░░█████           █████   ║
        ║    ░░░░░░░░░   ░░░░░░  ░░░░ ░░░░░ ░░░░ ░░░░░  ░░░░░░   ░░░░░░     ░░░░░           ░░░░░    ║
        ║                                                                                            ║
        ╚════════════════════════════════════════════════════════════════════════════════════════════╝""";
    System.out.println(header);
  }

  public static void requestEnter() {
    try (Scanner sin = new Scanner(System.in);) {
      sin.nextLine();
    } catch (Exception e) {
      System.out.println("[requestEnter]: " + e.getMessage());
    }
  }

  /**
   * Retrieves user input and prints request message
   *
   * @param message the message to be displayed to the user
   *
   * @return the input of the user as a string
   **/
  public static String getUserInput(String message) {
    System.out.print(message + " > ");

    return sin.nextLine();
  }

  // Source - https://stackoverflow.com/a
  // Posted by satish, modified by community. See post 'Timeline' for change
  // history
  // Retrieved 2025-11-26, License - CC BY-SA 4.0
  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  public static void main(String[] args) {
    displayBanner();
    String res = getUserInput("Please type in your username");
    clearScreen();
    getUserInput("");
  }
}
