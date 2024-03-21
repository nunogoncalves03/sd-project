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
    if (args.length == 6) {
      debugMode = args[5].equals("-debug");
    }

    // receive and print arguments
    debug("Received %d arguments\n", args.length);
    for (int i = 0; i < args.length; i++) {
      debug("arg[%d] = %s\n", i, args[i]);
    }

    // check arguments
    if (args.length < 5 || args.length > 6) {
      System.err.println("Invalid argument(s)!");
      System.err.println(
          "Usage: mvn exec:java -Dexec.args=<dns_host> <dns_port> <service> <sequencer_host> <sequencer_port> [-debug]");
      return;
    }

    final String dnsHost = args[0];
    final int dnsPort = Integer.parseInt(args[1]);
    final String service = args[2];
    final String sequencerHost = args[3];
    final int sequencerPort = Integer.parseInt(args[4]);

    final String dnsTarget = dnsHost + ":" + dnsPort;
    final String sequencerTarget = sequencerHost + ":" + sequencerPort;
    debug("DNS target: %s\n", dnsTarget);
    debug("Service: %s\n", service);
    debug("Sequencer target: %s\n", sequencerTarget);

    CommandProcessor parser =
        new CommandProcessor(new ClientService(dnsTarget, service, sequencerTarget));
    parser.parseInput();
    System.exit(0);
  }
}
