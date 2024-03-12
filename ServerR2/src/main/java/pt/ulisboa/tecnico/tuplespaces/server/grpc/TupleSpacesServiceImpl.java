package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import static io.grpc.Status.*;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.MatchingTuplesLockException;

public class TupleSpacesServiceImpl extends TupleSpacesReplicaImplBase {

  private final ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    try {
      ServerMain.debug("request @put: %s", request);

      final String tuple = request.getNewTuple();

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
  public void takePhase1(
      TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
    try {
      ServerMain.debug("request @takePhase1: %s", request);

      final String searchPattern = request.getSearchPattern();
      final int clientId = request.getClientId();

      List<String> result = state.takePhase1(searchPattern, clientId);

      TakePhase1Response response =
          TakePhase1Response.newBuilder().addAllReservedTuples(result).build();
      ServerMain.debug("response @takePhase1: %s", result.isEmpty() ? "empty\n" : response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      StatusRuntimeException responseException =
          INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @takePhase1: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (MatchingTuplesLockException e) {
      ServerMain.debug("Error @takePhase1: %s\n", e);
      StatusRuntimeException responseException =
          ABORTED.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @takePhase1: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (Exception e) {
      ServerMain.debug("Error @takePhase1: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @takePhase1: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }

  @Override
  public void takePhase1Release(
      TakePhase1ReleaseRequest request,
      StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
    try {
      ServerMain.debug("request @takePhase1Release: %s", request);

      final int clientId = request.getClientId();

      state.releaseClientLocks(clientId);

      TakePhase1ReleaseResponse response = TakePhase1ReleaseResponse.getDefaultInstance();
      ServerMain.debug("response @takePhase1Release: empty\n");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ServerMain.debug("Error @takePhase1Release: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @takePhase1Release: %s\n", responseException);
      responseObserver.onError(responseException);
    }
  }

  @Override
  public void takePhase2(
      TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
    try {
      ServerMain.debug("request @takePhase2: %s", request);

      final int clientId = request.getClientId();
      final String tuple = request.getTuple();

      state.takePhase2(tuple, clientId);

      TakePhase2Response response = TakePhase2Response.getDefaultInstance();
      ServerMain.debug("response @takePhase2: empty\n");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      StatusRuntimeException responseException =
          INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
      ServerMain.debug("response @takePhase2: %s\n", responseException);
      responseObserver.onError(responseException);
    } catch (Exception e) {
      ServerMain.debug("Error @takePhase2: %s\n", e);
      StatusRuntimeException responseException =
          INTERNAL.withDescription("Internal server error").asRuntimeException();
      ServerMain.debug("response @takePhase2: %s\n", responseException);
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
