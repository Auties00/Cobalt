package it.auties.whatsapp.protobuf.group;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

import it.auties.protobuf.encoder.ProtobufEncoder;
import lombok.*;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.ecc.ECKeyPair;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyRecord {
  private static final int MAX_STATES = 5;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("SenderKeyStateStructure")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private LinkedList<SenderKeyState> states;

  public boolean isEmpty() {
    return states.isEmpty();
  }

  public SenderKeyState senderKeyState() {
    return states.getFirst();
  }

  public SenderKeyState senderKeyState(int keyId) {
    return states().stream()
            .filter(key -> key.senderKeyId() == keyId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Cannot find key with id %s".formatted(keyId)));
  }

  public void senderKeyState(int id, int iteration, byte[] chainKey, byte[] signatureKey) {
    states.clear();
    states.add(new SenderKeyState(id, iteration, chainKey, signatureKey, null));
  }

  public void addSenderKeyState(int id, int iteration, byte[] chainKey, byte[] signatureKey) {
    states.addFirst(new SenderKeyState(id, iteration, chainKey, signatureKey, null));
    if (states.size() <= MAX_STATES) {
      return;
    }

    states.removeLast();
  }

  public byte[] serialize() {
    return ProtobufEncoder.encode(this);
  }
}
