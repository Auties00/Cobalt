package it.auties.whatsapp.model.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufIgnore;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage implements SignalSpecification {
  @JsonProperty("0")
  @ProtobufIgnore
  private int version;

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte @NonNull [] cipherText;

  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] signingKey;

  @JsonProperty("5")
  @ProtobufIgnore
  private byte[] signature;

  @JsonProperty("6")
  @ProtobufIgnore
  private byte[] serialized;

  public SenderKeyMessage(int id, int iteration, byte[] cipherText, byte[] signingKey) {
    this.version = CURRENT_VERSION;
    this.id = id;
    this.iteration = iteration;
    this.cipherText = cipherText;
    var encodedVersion = BytesHelper.versionToBytes(version);
    var encoded =  ProtobufEncoder.encode(this);
    var encodedMessage = Bytes.of(encodedVersion)
            .append(encoded);
    this.signature = Curve25519.calculateSignature(signingKey, encodedMessage.toByteArray());
    this.serialized = encodedMessage.append(signature).toByteArray();
  }

  public static SenderKeyMessage ofSerialized(byte[] serialized) {
    try {
      var buffer = Bytes.of(serialized);
      return ProtobufDecoder.forType(SenderKeyMessage.class)
              .decode(buffer.slice(1, -SIGNATURE_LENGTH).toByteArray())
              .version(BytesHelper.bytesToVersion(serialized[0]))
              .signature(buffer.slice(-SIGNATURE_LENGTH).toByteArray())
              .serialized(serialized);
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }
}
