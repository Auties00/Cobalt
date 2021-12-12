package it.auties.whatsapp.protobuf.signal.group;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyState {
  private static final int MAX_MESSAGE_KEYS = 2000;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int senderKeyId;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SenderChainKey")
  private SenderChainKey senderChainKey;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("SenderSigningKey")
  private SenderSigningKey senderSigningKey;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("SenderMessageKey")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @Builder.Default
  private List<SenderMessageKey> senderMessageKeys = new ArrayList<>();

  public SenderKeyState(int id, int iteration, byte[] chainKey, byte[] signatureKeyPublic, byte[] signatureKeyPrivate) {
    this.senderKeyId = id;
    this.senderChainKey = new SenderChainKey(iteration, chainKey);
    this.senderSigningKey = new SenderSigningKey(BinaryArray.of((byte) 5).append(signatureKeyPublic).data(), signatureKeyPrivate);
  }

  public byte[] signingKeyPublic() {
    return BinaryArray.of(senderSigningKey().publicKey()).slice(1).data();
  }

  public byte[] signingKeyPrivate() {
    return senderSigningKey().privateKey();
  }

  public boolean hasSenderMessageKey(int iteration) {
    return senderMessageKeys.stream()
            .anyMatch(senderMessageKey -> senderMessageKey.iteration() == iteration);
  }

  public void addSenderMessageKey(SenderMessageKey senderMessageKey) {
    senderMessageKeys.add(senderMessageKey);
    if (senderMessageKeys.size() <= MAX_MESSAGE_KEYS) {
      return;
    }

    senderMessageKeys.remove(0);
  }

  public SenderMessageKey removeSenderMessageKey(int iteration) {
    return senderMessageKeys.stream()
            .filter(key -> key.iteration() == iteration)
            .mapToInt(senderMessageKeys::indexOf)
            .mapToObj(senderMessageKeys::remove)
            .findFirst()
            .orElse(null);
  }
}
