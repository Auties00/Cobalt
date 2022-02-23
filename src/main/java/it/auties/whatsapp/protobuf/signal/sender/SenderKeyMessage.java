package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.util.VersionProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.Objects;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage implements VersionProvider {
  private static final int SIGNATURE_LENGTH = 64;

  private int version;

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] cipherText;

  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] signingKey;

  private byte[] signature;

  private byte[] serialized;

  public SenderKeyMessage(int id, int iteration, byte[] cipherText, byte[] signingKey) {
    this.version = CURRENT_VERSION;
    this.id = id;
    this.iteration = iteration;
    this.cipherText = cipherText;
    this.signingKey = signingKey;
    var encodedVersion = SignalHelper.serialize(version);
    var encoded =  ProtobufEncoder.encode(this);
    var encodedMessage = BinaryArray.of(encodedVersion)
            .append(encoded);
    this.signature = Curve.calculateSignature(signingKey, encodedMessage.data());
    this.serialized = encodedMessage.append(signature).data();
  }

  public static SenderKeyMessage ofEncoded(byte[] serialized) {
    try {
      return ProtobufDecoder.forType(SenderKeyMessage.class)
              .decode(copyOfRange(serialized, 1, serialized.length - SIGNATURE_LENGTH))
              .version(SignalHelper.deserialize(serialized[0]))
              .signature(copyOfRange(serialized, serialized.length - SIGNATURE_LENGTH, serialized.length))
              .serialized(copyOfRange(serialized, 0, serialized.length - SIGNATURE_LENGTH));
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }

  public byte[] serialized() {
    return Objects.requireNonNullElseGet(serialized,
            () -> this.serialized = serialize());
  }

  private byte[] serialize() {
    return BinaryArray.of(SignalHelper.serialize(version()))
            .append(encode(this))
            .append(signature())
            .data();
  }
}
