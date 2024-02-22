package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

public class ClientService {

  private final ManagedChannel channel;
  private TupleSpacesGrpc.TupleSpacesBlockingStub stub;

  public ClientService(String target){
    channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    stub = TupleSpacesGrpc.newBlockingStub(channel);
  }

  public void put(String newTuple){
    this.stub.put(PutRequest.newBuilder().setNewTuple(newTuple).build());
  }

  public String read(String searchPattern){
    return(this.stub.read(ReadRequest.newBuilder().setSearchPattern(searchPattern).build()).getResult());
  }

  public String take(String searchPattern){
    return(this.stub.take(TakeRequest.newBuilder().setSearchPattern(searchPattern).build()).getResult());
  }

  public List<String> getTupleSpacesState(){
    return(this.stub.getTupleSpacesState(getTupleSpacesStateRequest.getDefaultInstance()).getTupleList());
  }

  public void shutdownChannel(){
    channel.shutdownNow();
  }
    
}
