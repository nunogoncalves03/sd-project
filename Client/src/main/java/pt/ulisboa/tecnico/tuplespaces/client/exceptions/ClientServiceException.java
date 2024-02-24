package pt.ulisboa.tecnico.tuplespaces.client.exceptions;

import io.grpc.StatusRuntimeException;

public class ClientServiceException extends Exception {

  private String description;

  public ClientServiceException() {
    super();
    setDescription("Unknown error");
  }

  public ClientServiceException(StatusRuntimeException e) {
    super();
    setDescription(e.getStatus().getDescription());
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
