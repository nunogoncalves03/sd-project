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
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.SequencerServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

public class ClientService {
  private final ArrayList<ManagedChannel> channels = new ArrayList<>();
  private final ArrayList<TupleSpacesReplicaGrpc.TupleSpacesReplicaStub> stubs = new ArrayList<>();
  private final ArrayList<String> qualifiers = new ArrayList<>();
  private final DnsService dns;
  private final SequencerService sequencer;
  private final OrderedDelayer delayer;

  public ClientService(String dnsTarget, String service, String sequencerTarget) {
    dns = new DnsService(dnsTarget);
    sequencer = new SequencerService(sequencerTarget);
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

    int sequenceNumber = 0;
    try {
      sequenceNumber = sequencer.getSequenceNumber();
    } catch (SequencerServiceException e) {
      ClientMain.debug("Sequencer failed: %s\n", e);
      throw new ClientServiceException(e.getDescription());
    }

    PutRequest request =
        PutRequest.newBuilder().setNewTuple(newTuple).setSeqNumber(sequenceNumber).build();
    ClientMain.debug("sending Put requests with seqNumber %d\n", sequenceNumber);
    asyncSendRequests(
        (Integer index) -> stubs.get(index).put(request, new TupleSpacesObserver<>(collector)));

    try {
      collector.waitUntilAllReceived();
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
    ResponseCollector<TakeResponse> collector = new ResponseCollector<>(stubs.size());

    ClientMain.debug("calling procedure take('%s')\n", searchPattern);

    int sequenceNumber = 0;
    try {
      sequenceNumber = sequencer.getSequenceNumber();
    } catch (SequencerServiceException e) {
      ClientMain.debug("Sequencer failed: %s\n", e);
      throw new ClientServiceException(e.getDescription());
    }

    TakeRequest request =
        TakeRequest.newBuilder()
            .setSearchPattern(searchPattern)
            .setSeqNumber(sequenceNumber)
            .build();
    ClientMain.debug("sending Take requests with seqNumber %d\n", sequenceNumber);
    asyncSendRequests(
        (Integer index) -> stubs.get(index).take(request, new TupleSpacesObserver<>(collector)));

    try {
      TakeResponse response = collector.waitUntilAllReceived().get(0);
      ClientMain.debug("response: %s", response);
      return response.getResult();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    } catch (InterruptedException e) {
      throw new ClientServiceException(e);
    }
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

  public void shutdownChannels() {
    for (ManagedChannel channel : this.channels) {
      channel.shutdownNow();
    }
    ClientMain.debug("channels successfully shutdown\n");

    dns.shutdown();
  }
}
