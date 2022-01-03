package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.encoder.ProtobufEncoder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.NoSuchElementException;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyRecord {
  private static final int MAX_STATES = 5;

  @JsonProperty("1")
  @JsonPropertyDescription("SenderKeyStateStructure")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private LinkedList<SenderKeyState> states = new LinkedList<>();

  public boolean isEmpty() {
    return states.isEmpty();
  }

  public SenderKeyState senderKeyState() {
    return states.getFirst();
  }

  public SenderKeyState senderKeyState(int keyId) {
    return states().stream()
            .filter(key -> key.id() == keyId)
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
