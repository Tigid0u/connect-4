package ch.heigvd.connect4;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * This command only serves as an entry point and displays help by default
 **/
@Command(mixinStandardHelpOptions = true, version = "Connect4 v1.0", subcommands = {Connect4Server.class})
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
