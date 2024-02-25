package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class TupleSpacesServiceImpl extends TupleSpacesImplBase {

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  private final ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    try {
      ServerMain.debug("request @put: %s", request);

      final String tuple = request.getNewTuple();
      if (!isTupleValid(tuple)) {
        StatusRuntimeException responseException =
            INVALID_ARGUMENT.withDescription("Invalid tuple").asRuntimeException();
        ServerMain.debug("response @put: %s\n", responseException);
        responseObserver.onError(responseException);
        return;
      }

      state.put(tuple);

      PutResponse response = PutResponse.getDefaultInstance();
      ServerMain.debug("response @put: %s\n", response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
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
      if (!isPatternValidRegex(searchPattern)) {
        StatusRuntimeException responseException =
            INVALID_ARGUMENT.withDescription("Invalid search pattern").asRuntimeException();
        ServerMain.debug("response @read: %s\n", responseException);
        responseObserver.onError(responseException);
        return;
      }

      String result;
      result = state.read(searchPattern);

      ReadResponse response = ReadResponse.newBuilder().setResult(result).build();
      ServerMain.debug("response @read: %s", response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
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
      if (!isPatternValidRegex(searchPattern)) {
        StatusRuntimeException responseException =
            INVALID_ARGUMENT.withDescription("Invalid search pattern").asRuntimeException();
        ServerMain.debug("response @take: %s\n", responseException);
        responseObserver.onError(responseException);
        return;
      }

      String result;
      result = state.take(searchPattern);

      TakeResponse response = TakeResponse.newBuilder().setResult(result).build();
      ServerMain.debug("response @take: %s", response);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
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
      ServerMain.debug("request @getTupleSpacesState: %s\n", request);

      List<String> result = state.getTupleSpacesState();

      getTupleSpacesStateResponse response =
          getTupleSpacesStateResponse.newBuilder().addAllTuple(result).build();
      ServerMain.debug("response @getTupleSpacesState: %s", result.isEmpty() ? "\n" : response);

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

  private boolean isTupleValid(String tuple) {
    return tuple.startsWith(BGN_TUPLE) && tuple.endsWith(END_TUPLE);
  }

  private boolean isPatternValidRegex(String pattern) {
    try {
      Pattern.compile(pattern);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  }
}
