package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.Hkdf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderChainKey {
  private static final byte[] MESSAGE_KEY_SEED = {0x01};

  private static final byte[] CHAIN_KEY_SEED   = {0x02};

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] seed;

  public SenderMessageKey toSenderMessageKey() {
    return new SenderMessageKey(iteration, Hkdf.extract(seed, MESSAGE_KEY_SEED));
  }

  public SenderChainKey next() {
    return new SenderChainKey(iteration + 1, Hkdf.extract(seed, CHAIN_KEY_SEED));
  }
}
