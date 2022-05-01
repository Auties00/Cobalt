package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.signal.auth.KeyIndexList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyState implements ProtobufMessage {
  private static final int MAX_MESSAGE_KEYS = 2000;

  @ProtobufProperty(index = 1, type = UINT32)
  private int id;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = SenderChainKey.class)
  private SenderChainKey chainKey;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = SenderSigningKey.class)
  private SenderSigningKey signingKey;

  @ProtobufProperty(index = 4, type = MESSAGE,
          concreteType = SenderMessageKey.class, repeated = true)
  @Default
  private List<SenderMessageKey> messageKeys = new ArrayList<>();

  public SenderKeyState(int id, int iteration, byte[] chainKey, byte[] signatureKeyPublic, byte[] signatureKeyPrivate) {
    this.id = id;
    this.chainKey = new SenderChainKey(iteration, chainKey);
    this.signingKey = new SenderSigningKey(signatureKeyPublic, signatureKeyPrivate);
    this.messageKeys = new ArrayList<>();
  }

  public boolean hasSenderMessageKey(int iteration) {
    return messageKeys.stream()
            .anyMatch(senderMessageKey -> senderMessageKey.iteration() == iteration);
  }

  public void addSenderMessageKey(SenderMessageKey senderMessageKey) {
    messageKeys.add(senderMessageKey);
    if (messageKeys.size() <= MAX_MESSAGE_KEYS) {
      return;
    }

    messageKeys.remove(0);
  }

  public SenderMessageKey removeSenderMessageKey(int iteration) {
    return messageKeys.stream()
            .filter(key -> key.iteration() == iteration)
            .mapToInt(messageKeys::indexOf)
            .mapToObj(messageKeys::remove)
            .findFirst()
            .orElse(null);
  }

  public boolean equals(Object other){
    return other instanceof SenderKeyState that
            && Objects.equals(this.id(), that.id());
  }

  public static class SenderKeyStateBuilder {
    public SenderKeyStateBuilder messageKeys(List<SenderMessageKey> messageKeys) {
      this.messageKeys$set = true;
      if(messageKeys$value == null){
        this.messageKeys$value = messageKeys;
        return this;
      }

      this.messageKeys$value.addAll(messageKeys);
      return this;
    }
  }
}
