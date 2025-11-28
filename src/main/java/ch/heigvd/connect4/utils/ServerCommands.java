package ch.heigvd.connect4.utils;

/**
 * Defines the commands the server can send
 **/
public enum ServerCommands {
  // Feedback commands
  OK,
  ERROR,
  // Other commands
  GAME_STARTS,
  YOUR_TURN,
  END_OF_GAME
}
