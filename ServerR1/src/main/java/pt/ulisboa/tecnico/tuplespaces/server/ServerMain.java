package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.TupleSpacesServiceImpl;

public class ServerMain {

  public static boolean debugMode = false;

  public static void debug(String format, Object... args) {
    if (debugMode) {
      System.err.printf("[DEBUG tid=%s] ", Thread.currentThread().getId());
      System.err.printf(format, args);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println(ServerMain.class.getSimpleName());

    if (args.length == 2) {
      debugMode = args[1].equals("-debug");
    }

    // receive and print arguments
    debug("Received %d arguments\n", args.length);
    for (int i = 0; i < args.length; i++) {
      debug("arg[%d] = %s\n", i, args[i]);
    }

    // check arguments
    if (args.length < 1 || args.length > 2) {
      System.err.println("Invalid argument(s)!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<port> -debug");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final BindableService impl = new TupleSpacesServiceImpl();

    // Create a new server to listen on port
    Server server = ServerBuilder.forPort(port).addService(impl).build();

    // Start the server
    server.start();

    // Server threads are running in the background.
    debug("Server running on port %s\n", port);

    // Do not exit the main thread. Wait until server is terminated.
    server.awaitTermination();
    debug("Server was shutdown\n");
  }
}
