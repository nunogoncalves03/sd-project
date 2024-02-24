package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.ClientServiceException;

public class ClientService {

  private final ManagedChannel channel;
  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;

  public ClientService(String target) {
    channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    stub = TupleSpacesGrpc.newBlockingStub(channel);
  }

  public void put(String newTuple) throws ClientServiceException {
    try {
      this.stub.put(PutRequest.newBuilder().setNewTuple(newTuple).build());
    } catch (StatusRuntimeException e) {
      throw new ClientServiceException(e);
    }
  }

  public String read(String searchPattern) throws ClientServiceException {
    try {
      return (this.stub
          .read(ReadRequest.newBuilder().setSearchPattern(searchPattern).build())
          .getResult());
    } catch (StatusRuntimeException e) {
      throw new ClientServiceException(e);
    }
  }

  public String take(String searchPattern) throws ClientServiceException {
    try {
      return (this.stub
          .take(TakeRequest.newBuilder().setSearchPattern(searchPattern).build())
          .getResult());
    } catch (StatusRuntimeException e) {
      throw new ClientServiceException(e);
    }
  }

  public List<String> getTupleSpacesState() throws ClientServiceException {
    try {
      return (this.stub
          .getTupleSpacesState(getTupleSpacesStateRequest.getDefaultInstance())
          .getTupleList());
    } catch (StatusRuntimeException e) {
      throw new ClientServiceException(e);
    }
  }

  public void shutdownChannel() {
    channel.shutdownNow();
  }
}
