package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

import io.grpc.StatusRuntimeException;

public class DnsServiceException extends Exception {

  private String description;

  public DnsServiceException() {
    super();
    setDescription("Unknown error");
  }

  public DnsServiceException(StatusRuntimeException e) {
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
    return "DnsServiceException: " + description;
  }
}
