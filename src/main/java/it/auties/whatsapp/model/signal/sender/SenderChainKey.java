package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.crypto.Hmac;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderChainKey {
  private static final byte[] MESSAGE_KEY_SEED = {0x01};
  private static final byte[] CHAIN_KEY_SEED = {0x02};

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] seed;

  public SenderMessageKey toSenderMessageKey() {
    var hmac = Hmac.calculateSha256(MESSAGE_KEY_SEED, seed);
    return new SenderMessageKey(iteration, hmac);
  }

  public SenderChainKey next() {
    var hmac = Hmac.calculateSha256(CHAIN_KEY_SEED, seed);
    return new SenderChainKey(iteration + 1, hmac);
  }
}
