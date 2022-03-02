package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.buffer.ByteBuffer;
import it.auties.protobuf.annotation.ProtobufIgnore;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.crypto.SignalHelper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class SignalDistributionMessage implements SignalProtocolMessage {
  /**
   * The version of this message
   */
  @JsonProperty("0")
  @ProtobufIgnore
  private int version;

  /**
   * The jid of the sender
   */
  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  /**
   * The iteration of the message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  /**
   * The value key of the message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte @NonNull [] chainKey;

  /**
   * The signing key of the message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte @NonNull[] signingKey;

  /**
   * This message in a serialized form
   */
  @JsonProperty("5")
  @ProtobufIgnore
  private byte[] serialized;

  public SignalDistributionMessage(int id, int iteration, byte[] chainKey, byte[] signingKey) {
    this.version = CURRENT_VERSION;
    this.id = id;
    this.iteration = iteration;
    this.chainKey = chainKey;
    this.signingKey = signingKey;
    this.serialized = ByteBuffer.of(serializedVersion())
            .append(ProtobufEncoder.encode(this))
            .toByteArray();
  }

  public static SignalDistributionMessage ofSerialized(byte[] serialized){
    try {
      return ProtobufDecoder.forType(SignalDistributionMessage.class)
              .decode(Arrays.copyOfRange(serialized, 1, serialized.length))
              .version(SignalHelper.deserialize(serialized[0]))
              .serialized(serialized);
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }
}
