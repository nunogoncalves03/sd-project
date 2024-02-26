package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.server.ServerMain;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.DnsServiceException;

public class DnsService {
  private final ManagedChannel channel;
  private final NameServerGrpc.NameServerBlockingStub stub;

  public DnsService(String dnsTarget) {
    this.channel = ManagedChannelBuilder.forTarget(dnsTarget).usePlaintext().build();
    this.stub = NameServerGrpc.newBlockingStub(channel);
  }

  public void register(String target, String qualifier, String service) throws DnsServiceException {
    try {
      this.stub.register(
          RegisterRequest.newBuilder()
              .setAddress(target)
              .setQualifier(qualifier)
              .setService(service)
              .build());
    } catch (StatusRuntimeException e) {
      ServerMain.debug("RPC failed: %s\n", e.getStatus());
      throw new DnsServiceException(e);
    }
  }

  public void unregister(String target, String service) throws DnsServiceException {
    try {
      this.stub.delete(DeleteRequest.newBuilder().setAddress(target).setService(service).build());
    } catch (StatusRuntimeException e) {
      ServerMain.debug("RPC failed: %s\n", e.getStatus());
      throw new DnsServiceException(e);
    }

    channel.shutdownNow();
    ServerMain.debug("DNS channel successfully shutdown\n");
  }
}
