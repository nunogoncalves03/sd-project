package pt.ulisboa.tecnico.tuplespaces.client.exceptions;

import io.grpc.StatusRuntimeException;

public class SequencerServiceException extends Exception {

  private String description;

  public SequencerServiceException() {
    super();
    setDescription("Unknown error");
  }

  public SequencerServiceException(StatusRuntimeException e) {
    super();

    if (e.getStatus().getDescription() == null) {
      setDescription("Unknown error");
    } else {
      setDescription(e.getStatus().getDescription());
    }
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "SequencerServiceException: " + description;
  }
}
