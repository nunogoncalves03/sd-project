package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import static io.grpc.Status.*;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class TupleSpacesServiceImpl extends TupleSpacesReplicaImplBase {

  private final ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    try {
      ServerMain.debug("request @put: %s", request);

      final String tuple = request.getNewTuple();
      final int sequenceNumber = request.getSeqNumber();

      state.put(tuple);

      PutResponse response = PutResponse.getDefaultInstance();
      ServerMain.debug("response @put: empty\n");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      StatusRuntimeException responseException =
          INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @put: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (Exception e) {
      ServerMain.debug("Error @put: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @put: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }

  @Override
  public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
    try {
      ServerMain.debug("request @read: %s", request);

      final String searchPattern = request.getSearchPattern();

      String result = state.read(searchPattern);

      ReadResponse response = ReadResponse.newBuilder().setResult(result).build();
      ServerMain.debug("response @read: %s", response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      StatusRuntimeException responseException =
          INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @read: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (Exception e) {
      ServerMain.debug("Error @read: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @read: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }

  @Override
  public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
    try {
      ServerMain.debug("request @take: %s", request);

      final String searchPattern = request.getSearchPattern();
      final int sequenceNumber = request.getSeqNumber();

      String result = state.take(searchPattern);

      TakeResponse response = TakeResponse.newBuilder().setResult(result).build();
      ServerMain.debug("response @take: %s", response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      StatusRuntimeException responseException =
          INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @take: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (Exception e) {
      ServerMain.debug("Error @take: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @take: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }

  @Override
  public void getTupleSpacesState(
      getTupleSpacesStateRequest request,
      StreamObserver<getTupleSpacesStateResponse> responseObserver) {
    try {
      ServerMain.debug("request @getTupleSpacesState: empty\n");

      List<String> result = state.getTupleSpacesState();

      getTupleSpacesStateResponse response =
          getTupleSpacesStateResponse.newBuilder().addAllTuple(result).build();
      ServerMain.debug(
          "response @getTupleSpacesState: %s", result.isEmpty() ? "empty\n" : response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ServerMain.debug("Error @getTupleSpacesState: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @getTupleSpacesState: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }
}
