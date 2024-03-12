package pt.ulisboa.tecnico.tuplespaces.client.util;

import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;

public class ResponseCollector<R> {
  List<R> responses = new ArrayList<R>();
  int numberOfResponsesToWaitFor;
  int numberOfRejections = 0;
  StatusRuntimeException error = null;

  public enum Status {
    MINORITY_RECEIVED,
    MAJORITY_RECEIVED,
    ALL_RECEIVED
  }

  public ResponseCollector(int numberOfResponsesToWaitFor) {
    this.numberOfResponsesToWaitFor = numberOfResponsesToWaitFor;
  }

  public synchronized void addResponse(R response) {
    responses.add(response);
    notifyAll();
  }

  public synchronized void addRejection() {
    this.numberOfRejections++;
    notifyAll();
  }

  public synchronized List<R> getResponses() {
    return responses;
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

  public synchronized Status waitForResponsesOrMinority()
      throws InterruptedException, StatusRuntimeException {
    while (this.responses.size() + this.numberOfRejections != this.numberOfResponsesToWaitFor) {
      ClientMain.debug(
          "waiting for %s responses\n",
          this.numberOfResponsesToWaitFor - responses.size() - this.numberOfRejections);

      wait();

      if (this.error != null) {
        throw this.error;
      }
    }

    if (this.responses.size() == this.numberOfResponsesToWaitFor) {
      ClientMain.debug("received all responses (%s)\n", this.numberOfResponsesToWaitFor);
      return Status.ALL_RECEIVED;
    } else if (responses.size() > this.numberOfRejections) {
      ClientMain.debug(
          "received %s/%s responses (majority)\n",
          this.responses.size(), this.numberOfResponsesToWaitFor);
      return Status.MAJORITY_RECEIVED;
    } else {
      ClientMain.debug(
          "received %s/%s rejections (minority)\n",
          this.numberOfRejections, this.numberOfResponsesToWaitFor);
      return Status.MINORITY_RECEIVED;
    }
  }

  public synchronized void abort(StatusRuntimeException error) {
    this.error = error;
    notifyAll();
  }
}
