package it.auties.whatsapp.protobuf.model;

import com.fasterxml.jackson.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import it.auties.whatsapp.crypto.Hkdf;
import lombok.*;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.groups.ratchet.SenderMessageKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderChainKey {
  private static final byte[] MESSAGE_KEY_SEED = {0x01};

  private static final byte[] CHAIN_KEY_SEED   = {0x02};

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] seed;

  public SenderMessageKey toSenderMessageKey() {
    return new SenderMessageKey(iteration, Hkdf.extract(seed, MESSAGE_KEY_SEED));
  }

  public SenderChainKey next() {
    return new SenderChainKey(iteration + 1, Hkdf.extract(seed, CHAIN_KEY_SEED));
  }
}
