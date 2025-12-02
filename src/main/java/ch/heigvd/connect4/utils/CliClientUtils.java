package ch.heigvd.connect4.utils;

import java.util.Scanner;

import ch.heigvd.connect4.Connect4Client;

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

  /**
   * Retrieves user input as an integer within a specified range and prints
   * request message
   *
   * @param message the message to be displayed to the user
   * @param min     the minimum acceptable integer (inclusive)
   * @param max     the maximum acceptable integer (inclusive)
   *
   * @return the input of the user as an integer
   **/
  public static int getUserInputInt(String message, int min, int max) {
    int input = -1;
    boolean valid = false;

    while (!valid) {
      try {
        input = Integer.parseInt(getUserInput(message));
        if (input >= min && input <= max) {
          valid = true;
        } else {
          System.out.println("The number is out of range !");
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a valid number");
      }
    }
    return input;
  }

  // Source - https://stackoverflow.com/a
  // Posted by satish, modified by community. See post 'Timeline' for change
  // history
  // Retrieved 2025-11-26, License - CC BY-SA 4.0
  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  /**
   * Prints the Connect 4 board to the console
   *
   * @param grid 2D array representing the game board
   **/
  public static void printBoard(int[][] grid) {
    if (grid == null || grid.length == 0 || grid[0].length == 0) {
      throw new IllegalArgumentException("Grid must not be empty");
    }

    int rows = Connect4Client.HEIGHT;
    int cols = Connect4Client.WIDTH;

    // Top border
    printHorizontalBorder(cols, '┌', '┬', '┐');

    for (int r = 0; r < rows; r++) {
      // Row with pieces
      System.out.print('│');
      for (int c = 0; c < cols; c++) {
        char symbol = cellSymbol(grid[c][rows - 1 - r]);
        System.out.print(" " + symbol + " ");
        if (c < cols - 1) {
          System.out.print('│');
        }
      }
      System.out.println('│');

      // Separator line between rows (or bottom border after last row)
      if (r < rows - 1) {
        printHorizontalBorder(cols, '├', '┼', '┤');
      } else {
        printHorizontalBorder(cols, '└', '┴', '┘');
      }
    }
  }

  /**
   * Prints a horizontal border line for the board
   *
   * @param cols  number of columns in the board
   * @param left  character for the left corner
   * @param mid   character for the middle separators
   * @param right character for the right corner
   **/
  private static void printHorizontalBorder(int cols, char left, char mid, char right) {
    System.out.print(left);
    for (int c = 0; c < cols; c++) {
      System.out.print("───");
      if (c < cols - 1) {
        System.out.print(mid);
      }
    }
    System.out.println(right);
  }

  /**
   * Returns the symbol representing a cell's value
   *
   * @param value the value of the cell (0 = empty, 1 = player 1, 2 = player 2)
   * @return the character symbol for the cell
   **/
  private static char cellSymbol(int value) {
    return switch (value) {
      case 1 -> '●'; // Player 1
      case 2 -> '○'; // Player 2
      default -> ' '; // Empty
    };
  }

  public static void main(String[] args) {
    int[][] sampleGrid = {
        { 0, 0, 0, 0, 0, 0, 0 },
        { 0, 1, 2, 0, 0, 0, 0 },
        { 0, 1, 2, 0, 0, 0, 0 },
        { 0, 2, 1, 1, 0, 0, 0 },
        { 0, 1, 2, 2, 1, 0, 0 },
        { 2, 1, 1, 2, 1, 2, 0 }
    };
    displayBanner();
    printBoard(sampleGrid);
  }
}
