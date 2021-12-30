package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.util.BytesDeserializer;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.util.ByteUtil;

import java.io.IOException;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage {
  private static final int SIGNATURE_LENGTH = 64;
  private static final int CURRENT_VERSION = 3;

  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] cipherText;
  
  private int version;
  
  private byte[] serialized;

  public SenderKeyMessage(int id, int iteration, byte[] ciphertext) {
    this(id, iteration, ciphertext, 0, null);
  }

  public static SenderKeyMessage ofEncoded(byte[] serialized) {
    try {
      return ProtobufDecoder.forType(SenderKeyMessage.class)
              .decode(copyOfRange(serialized, 1, serialized.length - SIGNATURE_LENGTH))
              .version(Byte.toUnsignedInt(serialized[0]) >> 4)
              .serialized(serialized);
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }

  public SenderKeyMessage(int keyId, int iteration, byte[] ciphertext, byte[] signatureKey) {
    var version = (byte) ((CURRENT_VERSION << 4 | CURRENT_VERSION) & 0xFF);
    var message = ProtobufEncoder.encode(new SenderKeyMessage(keyId, iteration, ciphertext));
    var signature = Curve.calculateSignature(signatureKey, BinaryArray.of(version).append(message).data());
    this.serialized = BinaryArray.of(version).append(message).append(signature).data();
    this.version = CURRENT_VERSION;
    this.id = keyId;
    this.iteration = iteration;
    this.cipherText = ciphertext;
  }

  public void verifySignature(byte[] signatureKey) {
    var signatureKeyBinary = BinaryArray.of(signatureKey);
    var message = signatureKeyBinary.slice(serialized.length - SIGNATURE_LENGTH);
    var signature = signatureKeyBinary.slice(serialized.length - SIGNATURE_LENGTH, SIGNATURE_LENGTH);
    Validate.isTrue(Curve.verifySignature(signatureKey, message.data(), signature.data()), "Invalid signature");
  }

  public int type() {
    return CiphertextMessage.SENDERKEY_TYPE;
  }
}
