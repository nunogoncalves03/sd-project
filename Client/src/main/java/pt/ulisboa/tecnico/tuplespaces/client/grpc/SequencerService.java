package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.client.ClientMain;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.SequencerServiceException;

public class SequencerService {
  private final ManagedChannel channel;
  private final SequencerGrpc.SequencerBlockingStub stub;

  public SequencerService(String sequencerTarget) {
    this.channel = ManagedChannelBuilder.forTarget(sequencerTarget).usePlaintext().build();
    ClientMain.debug("Sequencer channel was created successfully\n");
    this.stub = SequencerGrpc.newBlockingStub(channel);
    ClientMain.debug("Sequencer stub was created successfully\n");
  }

  public int getSequenceNumber() throws SequencerServiceException {
    try {
      GetSeqNumberResponse response =
          this.stub.getSeqNumber(GetSeqNumberRequest.getDefaultInstance());

      ClientMain.debug("response: %s", response);
      return response.getSeqNumber();
    } catch (StatusRuntimeException e) {
      ClientMain.debug("RPC failed: %s\n", e.getStatus());
      throw new SequencerServiceException(e);
    }
  }

  public void shutdown() {
    channel.shutdownNow();
    ClientMain.debug("Sequencer channel successfully shutdown\n");
  }
}
