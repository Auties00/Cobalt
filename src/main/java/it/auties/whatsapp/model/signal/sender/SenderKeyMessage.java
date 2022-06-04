package it.auties.whatsapp.model.signal.sender;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyMessage implements ProtobufMessage, JacksonProvider, SignalSpecification {
  private int version;

  @ProtobufProperty(index = 1, type = UINT32)
  private Integer id;

  @ProtobufProperty(index = 2, type = UINT32)
  private Integer iteration;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte @NonNull [] cipherText;

  private byte[] signingKey;

  private byte[] signature;

  private byte[] serialized;

  @SneakyThrows
  public SenderKeyMessage(int id, int iteration, byte @NonNull [] cipherText, byte @NonNull [] signingKey) {
    this.version = CURRENT_VERSION;
    this.id = id;
    this.iteration = iteration;
    this.cipherText = cipherText;
    this.signingKey = signingKey;
    var serialized = Bytes.of(BytesHelper.versionToBytes(version))
            .append(PROTOBUF.writeValueAsBytes(this));
    this.signature = Curve25519.sign(signingKey, serialized.toByteArray(), true);
    this.serialized = serialized.append(signature)
            .toByteArray();
  }

  @SneakyThrows
  public static SenderKeyMessage ofSerialized(byte[] serialized) {
    var buffer = Bytes.of(serialized);
    return PROTOBUF.readMessage(buffer.slice(1, -SIGNATURE_LENGTH).toByteArray(), SenderKeyMessage.class)
            .version(BytesHelper.bytesToVersion(serialized[0]))
            .signature(buffer.slice(-SIGNATURE_LENGTH).toByteArray())
            .serialized(serialized);
  }
}
