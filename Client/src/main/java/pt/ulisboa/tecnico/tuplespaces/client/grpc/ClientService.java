package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import main.java.pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.ClientServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.DnsServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;

public class ClientService {
  private final Map<String, ManagedChannel> channels = new HashMap<>();
  private final Map<String, TupleSpacesGrpc.TupleSpacesStub> stubs = new HashMap<>();
  private final Map<String, Integer> qualifier_ix = new HashMap<>();
  private final DnsService dns;
  private final OrderedDelayer delayer;

  public ClientService(String target, String service) {
    dns = new DnsService(target);
    String[] qualifiers = {"A", "B", "C"};


    for (String qualifier : qualifiers) {
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
      TupleSpacesGrpc.TupleSpacesStub stub = TupleSpacesGrpc.newStub(channel);
      ClientMain.debug("server '%s' stub was created successfully\n", qualifier);

      qualifier_ix.put(qualifier, stubs.size());
      channels.put(qualifier, channel);
      stubs.put(qualifier, stub);
    }

    if (stubs.isEmpty()) {
      System.err.println("No available servers");
      System.exit(1);
    }

    delayer = new OrderedDelayer(stubs.size());
  }
  

  public void put(String newTuple) throws ClientServiceException {
    String qualifier;

    ResponseCollector<PutResponse> collector = new ResponseCollector<>(stubs.size());

    ClientMain.debug("calling procedure put('%s')\n", newTuple);
    PutRequest request = PutRequest.newBuilder().setNewTuple(newTuple).build();
    for(Integer ix : delayer){
      qualifier = qualifier_ix.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == ix)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
      stubs.get(qualifier).put(request, new TupleSpacesObserver<>(collector));
      ClientMain.debug("send request to server %s\n", qualifier);
    }

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
    String qualifier;

    ResponseCollector<ReadResponse> collector = new ResponseCollector<>(1);

    ClientMain.debug("calling procedure read('%s')\n", searchPattern);
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(searchPattern).build();
    for(Integer ix : delayer){
      qualifier = qualifier_ix.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == ix)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
      stubs.get(qualifier).read(request, new TupleSpacesObserver<>(collector));
      ClientMain.debug("send request to server %s\n", qualifier);
    }

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
    throw new ClientServiceException("Not implemented");
    //    try {
    //      ClientMain.debug("calling procedure take('%s')\n", searchPattern);
    //      TakeResponse response =
    //          this.stub.take(TakeRequest.newBuilder().setSearchPattern(searchPattern).build());
    //      ClientMain.debug("response: %s", response);
    //      return response.getResult();
    //    } catch (StatusRuntimeException e) {
    //      ClientMain.debug("RPC failed: %s\n", e.getStatus());
    //      throw new ClientServiceException(e);
    //    }
  }

  public List<String> getTupleSpacesState(String qualifier) throws ClientServiceException {
    if (!stubs.containsKey(qualifier)) {
      throw new ClientServiceException("Server not available");
    }

    for(Integer ix : delayer){
      if(qualifier_ix.get(qualifier) == ix)
        break;
    }

    ResponseCollector<getTupleSpacesStateResponse> collector = new ResponseCollector<>(1);

    ClientMain.debug("calling procedure getTupleSpacesState('%s')\n", qualifier);
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();
    stubs.get(qualifier).getTupleSpacesState(request, new TupleSpacesObserver<>(collector));

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

  public void shutdownChannels() {
    for (ManagedChannel channel : this.channels.values()) {
      channel.shutdownNow();
    }
    ClientMain.debug("channels successfully shutdown\n");

    dns.shutdown();
  }

  public void setDelay(String qualifier, Integer time) throws ClientServiceException{
    if(!qualifier_ix.containsKey(qualifier)){
      throw new ClientServiceException("Server not available");
    }
    delayer.setDelay(qualifier_ix.get(qualifier), time);
  }
}
