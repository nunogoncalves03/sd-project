package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.DnsServiceException;

public class DnsService {
    private final ManagedChannel channel;
    private final NameServerGrpc.NameServerBlockingStub stub;

    public DnsService(String dnsTarget) {
        this.channel = ManagedChannelBuilder.forTarget(dnsTarget).usePlaintext().build();
        ClientMain.debug("DNS channel was created successfully\n");
        this.stub = NameServerGrpc.newBlockingStub(channel);
        ClientMain.debug("DNS stub was created successfully\n");
    }

    public List<String> lookup(String service, String qualifier) throws DnsServiceException {
        try {
            LookupResponse response;

            if (qualifier != null) {
                response = this.stub.lookup(LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build());
            } else {
                response = this.stub.lookup(LookupRequest.newBuilder().setService(service).build());
            }

            ClientMain.debug("response: %s", response.getAddressCount() == 0 ? "\n" : response);
            return response.getAddressList();
        } catch (StatusRuntimeException e) {
            ClientMain.debug("RPC failed: %s\n", e.getStatus());
            throw new DnsServiceException(e);
        }
    }

    public void shutdown() {
        channel.shutdownNow();
        ClientMain.debug("DNS channel successfully shutdown\n");
    }
}
