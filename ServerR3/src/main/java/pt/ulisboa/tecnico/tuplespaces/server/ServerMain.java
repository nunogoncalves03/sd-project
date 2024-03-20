package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.DnsServiceException;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.DnsService;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.TupleSpacesServiceImpl;

public class ServerMain {

  public static boolean debugMode = false;

  public static synchronized void debug(String format, Object... args) {
    if (debugMode) {
      System.err.printf("[DEBUG tid=%s] ", Thread.currentThread().getId());
      System.err.printf(format, args);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length == 7) {
      debugMode = args[6].equals("-debug");
    }

    // receive and print arguments
    debug("Received %d arguments\n", args.length);
    for (int i = 0; i < args.length; i++) {
      debug("arg[%d] = %s\n", i, args[i]);
    }

    // check arguments
    if (args.length < 6 || args.length > 7) {
      System.err.println("Invalid argument(s)!");
      System.err.println(
          "Usage: mvn exec:java -Dexec.args=<dns_host> <dns_port> <server_host> <server_port> <qualifier> <service> [-debug]");
      return;
    }

    final String dnsHost = args[0];
    final int dnsPort = Integer.parseInt(args[1]);
    final String serverHost = args[2];
    final int serverPort = Integer.parseInt(args[3]);
    final String qualifier = args[4];
    final String service = args[5];

    final String dnsTarget = dnsHost + ":" + dnsPort;
    final String target = serverHost + ":" + serverPort;

    DnsService dns = new DnsService(dnsTarget);

    try {
      dns.register(target, qualifier, service);
      debug("Server registered on DNS service\n");
    } catch (DnsServiceException e) {
      System.err.println("DNS registration failed: " + e.getDescription());
      System.exit(1);
    }

    final BindableService impl = new TupleSpacesServiceImpl();

    // Create a new server to listen on port
    Server server = ServerBuilder.forPort(serverPort).addService(impl).build();

    // Start the server
    server.start();

    // Server threads are running in the background.
    System.out.printf("Server running on port %s\n", serverPort);
    System.out.println("Press CTRL+C to terminate");

    Thread shutdownHook =
        new Thread(
            () -> {
              server.shutdown();

              try {
                dns.unregister(target, service);
                debug("Server unregistered from DNS service\n");

                dns.shutdown();
              } catch (DnsServiceException e) {
                System.err.println("DNS unregister failed: " + e.getDescription());
                dns.shutdown();
              }
            });
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // Do not exit the main thread. Wait until server is terminated.
    server.awaitTermination();
    System.out.println("Server was shutdown");
  }
}
