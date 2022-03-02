package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.buffer.ByteBuffer;
import it.auties.protobuf.annotation.ProtobufIgnore;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.util.SignalProvider;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;

import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage implements SignalProvider {
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
    this.signingKey = signingKey;
    var encodedVersion = SignalHelper.serialize(version);
    var encoded =  ProtobufEncoder.encode(this);
    var encodedMessage = ByteBuffer.of(encodedVersion)
            .append(encoded);
    this.signature = Curve.calculateSignature(signingKey, encodedMessage.toByteArray());
    this.serialized = encodedMessage.append(signature).toByteArray();
  }

  public static SenderKeyMessage ofSerialized(byte[] serialized) {
    try {
      return ProtobufDecoder.forType(SenderKeyMessage.class)
              .decode(copyOfRange(serialized, 1, serialized.length - SIGNATURE_LENGTH))
              .version(SignalHelper.deserialize(serialized[0]))
              .signature(copyOfRange(serialized, serialized.length - SIGNATURE_LENGTH, serialized.length))
              .serialized(serialized);
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }
}
