package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.util.ResponseCollector;

public class TupleSpacesObserver<R> implements StreamObserver<R> {
  ResponseCollector<R> collector;

  public TupleSpacesObserver(ResponseCollector<R> collector) {
    super();

    this.collector = collector;
  }

  @Override
  public void onNext(R response) {
    collector.addResponse(response);
  }

  @Override
  public void onError(Throwable throwable) {
    this.collector.abort(Status.fromThrowable(throwable).asRuntimeException());
  }

  @Override
  public void onCompleted() {}
}
