package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.List;

public class TupleSpacesServiceImpl extends TupleSpacesImplBase {

  private ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {

    state.put(request.getNewTuple());

    PutResponse response = PutResponse.getDefaultInstance();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {

    String result;
    try {
      result = state.read(request.getSearchPattern());
    } catch (InterruptedException e) {
      e.printStackTrace();
      responseObserver.onError(e);
      return;
    }

    ReadResponse response = ReadResponse.newBuilder().setResult(result).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
    String result;
    try {
      result = state.take(request.getSearchPattern());
    } catch (InterruptedException e) {
      e.printStackTrace();
      responseObserver.onError(e);
      return;
    }

    TakeResponse response = TakeResponse.newBuilder().setResult(result).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {

    List<String> result = state.getTupleSpacesState();

    getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(result).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
