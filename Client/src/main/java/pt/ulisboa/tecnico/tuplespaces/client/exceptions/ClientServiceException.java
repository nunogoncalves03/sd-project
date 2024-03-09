package pt.ulisboa.tecnico.tuplespaces.client.exceptions;

import io.grpc.StatusRuntimeException;

public class ClientServiceException extends Exception {

  private String description;

  public ClientServiceException() {
    super();
    setDescription("Unknown error");
  }

  public ClientServiceException(String description) {
    super();

    setDescription(description);
  }

  public ClientServiceException(StatusRuntimeException e) {
    super();

    if (e.getStatus().getDescription() == null) {
      setDescription("Unknown error");
    } else {
      setDescription(e.getStatus().getDescription());
    }
  }

  public ClientServiceException(InterruptedException e) {
    super();

    setDescription(e.getMessage());
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ClientServiceException: " + description;
  }
}
