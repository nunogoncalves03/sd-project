package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import main.java.pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.ClientServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.DnsServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;

public class ClientService {
  private final ArrayList<ManagedChannel> channels = new ArrayList<>();
  private final ArrayList<TupleSpacesReplicaGrpc.TupleSpacesReplicaStub> stubs = new ArrayList<>();
  private final ArrayList<String> qualifiers = new ArrayList<>();
  private final DnsService dns;
  private final OrderedDelayer delayer;

  public ClientService(String target, String service) {
    dns = new DnsService(target);
    String[] defaultQualifiers = {"A", "B", "C"};

    for (String qualifier : defaultQualifiers) {
      List<String> addresses = null;
      try {
        addresses = dns.lookup(service, qualifier);
        ClientMain.debug("DNS lookup successful: %s\n", addresses);
      } catch (DnsServiceException e) {
        System.err.println("DNS lookup failed: " + e.getDescription());
        System.exit(1);
      }

      if (addresses.isEmpty()) {
        ClientMain.debug("Server '%s' isn't available\n", qualifier);
        continue;
      }

      String address = addresses.get(0);

      ClientMain.debug("connecting to server '%s' @%s\n", qualifier, address);
      ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
      ClientMain.debug("server '%s' channel was created successfully\n", qualifier);
      TupleSpacesReplicaGrpc.TupleSpacesReplicaStub stub = TupleSpacesReplicaGrpc.newStub(channel);
      ClientMain.debug("server '%s' stub was created successfully\n", qualifier);

      qualifiers.add(qualifier);
      channels.add(channel);
      stubs.add(stub);
    }

    if (stubs.isEmpty()) {
      System.err.println("No available servers");
      System.exit(1);
    }

    delayer = new OrderedDelayer(qualifiers.size());
  }

  public void put(String newTuple) throws ClientServiceException {
    ResponseCollector<PutResponse> collector = new ResponseCollector<>(stubs.size());

    ClientMain.debug("calling procedure put('%s')\n", newTuple);
    PutRequest request = PutRequest.newBuilder().setNewTuple(newTuple).build();
    ClientMain.debug("sending Put requests\n");
    asyncSendRequests(
        (Integer index) -> stubs.get(index).put(request, new TupleSpacesObserver<>(collector)));

    try {
      List<PutResponse> responses = collector.waitUntilAllReceived();
      ClientMain.debug("all servers acknowledged\n");
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
  }

  public String read(String searchPattern) throws ClientServiceException {
    ResponseCollector<ReadResponse> collector = new ResponseCollector<>(1);

    ClientMain.debug("calling procedure read('%s')\n", searchPattern);
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(searchPattern).build();

    ClientMain.debug("sending Read requests\n");
    asyncSendRequests(
        (Integer index) -> stubs.get(index).read(request, new TupleSpacesObserver<>(collector)));

    try {
      ReadResponse response = collector.waitUntilAllReceived().get(0);
      ClientMain.debug("response: %s", response);
      return response.getResult();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
  }

  public String take(String searchPattern) throws ClientServiceException {
    ResponseCollector.Status status = null;
    ResponseCollector<TakePhase1Response> collector = null;
    TakePhase1Request request =
        TakePhase1Request.newBuilder()
            .setSearchPattern(searchPattern)
            .setClientId(ClientMain.id)
            .build();
    String tupleToTake = null;

    while (status != ResponseCollector.Status.ALL_RECEIVED) {
      try {
        collector = new ResponseCollector<>(stubs.size());
        status = takePhase1(collector, request);

        if (status == ResponseCollector.Status.MAJORITY_RECEIVED) {
          randomSleep();
        } else if (status == ResponseCollector.Status.MINORITY_RECEIVED) {
          releaseAllLocks();
          randomSleep();
        } else if (status == ResponseCollector.Status.ALL_RECEIVED) {
          if ((tupleToTake = chooseTupleFromIntersection(collector)) == null) {
            status = null;
            randomSleep();
          }
        } else {
          System.err.println("Unknown status, shouldn't happen");
          System.exit(1);
        }
      } catch (StatusRuntimeException e) {
        ClientMain.debug("RPC failed: %s\n", e.getStatus());
        throw new ClientServiceException(e);
      } catch (InterruptedException e) {
        throw new ClientServiceException(e);
      }
    }

    return takePhase2(tupleToTake);
  }

  private ResponseCollector.Status takePhase1(
      ResponseCollector<TakePhase1Response> collector, TakePhase1Request request)
      throws ClientServiceException {
    try {
      ClientMain.debug("sending TakePhase1 requests\n");
      asyncSendRequests(
          (Integer index) ->
              stubs.get(index).takePhase1(request, new TupleSpacesObserver<>(collector)));
      return collector.waitForResponsesOrMinority();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
  }

  private String takePhase2(String tupleToTake) throws ClientServiceException {

    TakePhase2Request request =
        TakePhase2Request.newBuilder().setClientId(ClientMain.id).setTuple(tupleToTake).build();

    ResponseCollector<TakePhase2Response> collector = new ResponseCollector<>(stubs.size());

    ClientMain.debug("sending TakePhase2 requests\n");
    asyncSendRequests(
        (Integer index) ->
            stubs.get(index).takePhase2(request, new TupleSpacesObserver<>(collector)));

    try {
      collector.waitUntilAllReceived();
      ClientMain.debug("all servers acknowledged the TakePhase2 request\n");
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }

    return tupleToTake;
  }

  public List<String> getTupleSpacesState(String qualifier) throws ClientServiceException {
    if (!qualifiers.contains(qualifier)) {
      throw new ClientServiceException("Server not available");
    }

    ResponseCollector<getTupleSpacesStateResponse> collector = new ResponseCollector<>(1);

    ClientMain.debug("calling procedure getTupleSpacesState('%s')\n", qualifier);
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();
    stubs
        .get(qualifiers.indexOf(qualifier))
        .getTupleSpacesState(request, new TupleSpacesObserver<>(collector));

    try {
      getTupleSpacesStateResponse response = collector.waitUntilAllReceived().get(0);

      ClientMain.debug("response: %s", response.getTupleCount() == 0 ? "empty\n" : response);
      return response.getTupleList();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
  }

  public void setDelay(String qualifier, Integer time) throws ClientServiceException {
    if (!qualifiers.contains(qualifier)) {
      throw new ClientServiceException("Server not available");
    }
    delayer.setDelay(qualifiers.indexOf(qualifier), time);
  }

  private void asyncSendRequests(Consumer<Integer> runnable) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.execute(
        () -> {
          for (Integer index : delayer) {
            runnable.accept(index);
            ClientMain.debug("send request to server %s\n", qualifiers.get(index));
          }
        });
  }

  private void releaseAllLocks() throws ClientServiceException {
    ResponseCollector<TakePhase1ReleaseResponse> collector = new ResponseCollector<>(stubs.size());
    TakePhase1ReleaseRequest request =
        TakePhase1ReleaseRequest.newBuilder().setClientId(ClientMain.id).build();

    ClientMain.debug("sending TakePhase1Release requests\n");
    asyncSendRequests(
        (Integer index) ->
            stubs.get(index).takePhase1Release(request, new TupleSpacesObserver<>(collector)));

    try {
      collector.waitUntilAllReceived();
      ClientMain.debug("all servers acknowledged the lock release request\n");
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
  }

  private String chooseTupleFromIntersection(ResponseCollector<TakePhase1Response> collector) {
    Set<String> tuplesSet = new HashSet<>(collector.getResponses().get(0).getReservedTuplesList());

    for (TakePhase1Response response : collector.getResponses()) {
      tuplesSet.retainAll(response.getReservedTuplesList());
    }

    final List<String> intersection = new ArrayList<>(tuplesSet);

    ClientMain.debug("tuple intersection: %s\n", intersection);

    if (intersection.isEmpty()) {
      return null;
    }

    final String chosenTuple = intersection.get(0);

    ClientMain.debug("chosen tuple to take: %s\n", chosenTuple);

    return chosenTuple;
  }

  private void randomSleep() throws InterruptedException {
    Thread.sleep((long) (Math.random() * 1000));
  }

  public void shutdownChannels() {
    for (ManagedChannel channel : this.channels) {
      channel.shutdownNow();
    }
    ClientMain.debug("channels successfully shutdown\n");

    dns.shutdown();
  }
}
