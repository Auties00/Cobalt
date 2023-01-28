package it.auties.whatsapp.model.signal.sender;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.Specification;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record SenderMessageKey(int iteration, byte[] seed, byte[] iv, byte[] cipherKey)
    implements ProtobufMessage {

  public SenderMessageKey(int iteration, byte[] seed) {
    this(iteration, seed, createIv(seed), createCipherKey(seed));
  }

  private static byte[] createIv(byte[] seed) {
    var derivative = getDerivedSeed(seed);
    return Bytes.of(derivative[0])
        .cut(Specification.Signal.IV_LENGTH)
        .toByteArray();
  }

  private static byte[] createCipherKey(byte[] seed) {
    var derivative = getDerivedSeed(seed);
    return Bytes.of(derivative[0])
        .slice(Specification.Signal.IV_LENGTH)
        .append(derivative[1])
        .cut(Specification.Signal.KEY_LENGTH)
        .toByteArray();
  }

  private static byte[][] getDerivedSeed(byte[] seed) {
    return Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
  }
}