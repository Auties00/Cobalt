package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.util.SignalSpecification;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Builder
@Jacksonized
@Data
@Accessors(fluent = true)
public class SenderKeyState
    implements ProtobufMessage, SignalSpecification {

  private final int id;
  private final SignalKeyPair signingKey;
  private final ConcurrentHashMap<Integer, SenderMessageKey> messageKeys;
  private SenderChainKey chainKey;

  public SenderKeyState(int id, int iteration, byte[] seed, SignalKeyPair signingKey) {
    this.id = id;
    this.chainKey = new SenderChainKey(iteration, seed);
    this.signingKey = signingKey;
    this.messageKeys = new ConcurrentHashMap<>();
  }

  public void addSenderMessageKey(SenderMessageKey senderMessageKey) {
    messageKeys.put(senderMessageKey.iteration(), senderMessageKey);
  }

  public Optional<SenderMessageKey> findSenderMessageKey(int iteration) {
    return Optional.ofNullable(messageKeys.get(iteration));
  }

  public boolean equals(Object other) {
    return other instanceof SenderKeyState that && Objects.equals(this.id(), that.id());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id());
  }
}
