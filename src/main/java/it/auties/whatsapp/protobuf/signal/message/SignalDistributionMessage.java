package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.SignalHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class SignalDistributionMessage implements SignalProtocolMessage {
  /**
   * The version of this message
   */
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
  private byte[] chainKey;

  /**
   * The signing key of the message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] signingKey;

  /**
   * This message in a serialized form
   */
  private byte[] serialized;

  public SignalDistributionMessage(int id, int iteration, byte[] chainKey, byte[] signingKey) {
    this(CURRENT_VERSION, id, iteration, chainKey, signingKey, null);
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

  public byte[] serialized() {
    return Objects.requireNonNullElseGet(serialized,
            () -> this.serialized = serialize());
  }

  private byte[] serialize() {
    return BinaryArray.of(serializedVersion())
            .append(ProtobufEncoder.encode(this))
            .data();
  }
}
