package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.ClientServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.DnsServiceException;

public class ClientService {

  private final ManagedChannel channel;
  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
  private final DnsService dns;

  public ClientService(String target, String service) {
    dns = new DnsService(target);
    List<String> addresses = null;

    try {
      addresses = dns.lookup(service, null);
      ClientMain.debug("DNS lookup successful: %s\n", addresses);
    } catch (DnsServiceException e) {
      System.err.println("DNS lookup failed: " + e.getDescription());
      System.exit(1);
    }

    if (addresses.isEmpty()) {
      System.err.println("No available servers");
      System.exit(1);
    }

    String address = addresses.get(0);
    ClientMain.debug("connecting to service @%s\n", address);

    channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
    ClientMain.debug("service channel was created successfully\n");
    stub = TupleSpacesGrpc.newBlockingStub(channel);
    ClientMain.debug("service stub was created successfully\n");
  }

  public void put(String newTuple) throws ClientServiceException {
    try {
      ClientMain.debug("calling procedure put('%s')\n", newTuple);
      PutResponse response = this.stub.put(PutRequest.newBuilder().setNewTuple(newTuple).build());
      ClientMain.debug("response: %s\n", response);
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    }
  }

  public String read(String searchPattern) throws ClientServiceException {
    try {
      ClientMain.debug("calling procedure read('%s')\n", searchPattern);
      ReadResponse response =
          this.stub.read(ReadRequest.newBuilder().setSearchPattern(searchPattern).build());
      ClientMain.debug("response: %s", response);
      return response.getResult();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    }
  }

  public String take(String searchPattern) throws ClientServiceException {
    try {
      ClientMain.debug("calling procedure take('%s')\n", searchPattern);
      TakeResponse response =
          this.stub.take(TakeRequest.newBuilder().setSearchPattern(searchPattern).build());
      ClientMain.debug("response: %s", response);
      return response.getResult();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    }
  }

  public List<String> getTupleSpacesState() throws ClientServiceException {
    try {
      ClientMain.debug("calling procedure getTupleSpacesState()\n");
      getTupleSpacesStateResponse response =
          this.stub.getTupleSpacesState(getTupleSpacesStateRequest.getDefaultInstance());
      ClientMain.debug("response: %s", response.getTupleCount() == 0 ? "\n" : response);
      return response.getTupleList();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new ClientServiceException(e);
    }
  }

  public void shutdownChannel() {
    channel.shutdownNow();
    ClientMain.debug("channel successfully shutdown\n");

    dns.shutdown();
  }
}
