package ch.heigvd.connect4;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import ch.heigvd.connect4.Connect4Client;

/**
 * This command only serves as an entry point and displays help by default
 **/
@Command(mixinStandardHelpOptions = true, version = "Connect4 v1.0", subcommands = { Connect4Client.class })
class Root implements Runnable {
  public void run() {
    CommandLine.usage(this, System.out);
  }
}

public class Main {
  public static void main(String[] args) {
    new CommandLine(new Root()).execute(args);
  }
}
