package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  public static void main(String[] args) {
    System.out.println(ClientMain.class.getSimpleName());

    // receive and print arguments
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // check arguments
    if (args.length != 3) {
      System.err.println("Argument(s) missing!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port>");
      return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);
    final String target = host + ":" + port;
    System.out.println("Target: " + target);

    CommandProcessor parser = new CommandProcessor(new ClientService(target));
    parser.parseInput();
  }
}
