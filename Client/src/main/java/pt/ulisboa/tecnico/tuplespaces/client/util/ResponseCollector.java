package pt.ulisboa.tecnico.tuplespaces.client.util;

import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;

public class ResponseCollector<R> {
  List<R> responses = new ArrayList<R>();
  int numberOfResponsesToWaitFor;
  StatusRuntimeException error = null;

  public ResponseCollector(int numberOfResponsesToWaitFor) {
    this.numberOfResponsesToWaitFor = numberOfResponsesToWaitFor;
  }

  public synchronized void addResponse(R response) {
    responses.add(response);
    notifyAll();
  }

  public synchronized List<R> waitUntilAllReceived()
      throws InterruptedException, StatusRuntimeException {
    while (responses.size() < this.numberOfResponsesToWaitFor) {
      ClientMain.debug(
          "waiting for %s responses\n", this.numberOfResponsesToWaitFor - responses.size());

      wait();

      if (this.error != null) {
        throw this.error;
      }
    }
    ClientMain.debug("all responses received (%s)\n", this.numberOfResponsesToWaitFor);

    return new ArrayList<>(this.responses.subList(0, this.numberOfResponsesToWaitFor));
  }

  public synchronized void abort(StatusRuntimeException error) {
    this.error = error;
    notifyAll();
  }
}
