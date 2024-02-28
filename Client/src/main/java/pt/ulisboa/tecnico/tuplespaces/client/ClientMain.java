package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  public static boolean debugMode = false;

  public static void debug(String format, Object... args) {
    if (debugMode) {
      System.err.print("[DEBUG] ");
      System.err.printf(format, args);
    }
  }

  public static void main(String[] args) {
    if (args.length == 4) {
      debugMode = args[3].equals("-debug");
    }

    // receive and print arguments
    debug("Received %d arguments\n", args.length);
    for (int i = 0; i < args.length; i++) {
      debug("arg[%d] = %s\n", i, args[i]);
    }

    // check arguments
    if (args.length < 3 || args.length > 4) {
      System.err.println("Invalid argument(s)!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port> <service> [-debug]");
      return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);
    final String service = args[2];
    final String target = host + ":" + port;
    debug("DNS target: %s\n", target);
    debug("Service: %s\n", service);

    CommandProcessor parser = new CommandProcessor(new ClientService(target, service));
    parser.parseInput();
  }
}
