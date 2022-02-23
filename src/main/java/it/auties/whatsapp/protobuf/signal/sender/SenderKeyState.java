package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderKeyState {
  private static final int MAX_MESSAGE_KEYS = 2000;

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty("2")
  @JsonPropertyDescription("SenderChainKey")
  private SenderChainKey chainKey;

  @JsonProperty("3")
  @JsonPropertyDescription("SenderSigningKey")
  private SenderSigningKey signingKey;

  @JsonProperty("4")
  @JsonPropertyDescription("SenderMessageKey")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @Default
  private List<SenderMessageKey> messageKeys = new ArrayList<>();

  public SenderKeyState(int id, int iteration, byte[] chainKey, byte[] signatureKeyPublic, byte[] signatureKeyPrivate) {
    this.id = id;
    this.chainKey = new SenderChainKey(iteration, chainKey);
    this.signingKey = new SenderSigningKey(signatureKeyPublic, signatureKeyPrivate);
    this.messageKeys = new ArrayList<>();
  }

  public byte[] signingKeyPublic() {
    return signingKey().publicKey();
  }

  public byte[] signingKeyPrivate() {
    return signingKey().privateKey();
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
}
