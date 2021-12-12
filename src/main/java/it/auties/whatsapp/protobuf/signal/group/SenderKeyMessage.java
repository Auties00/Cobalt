package it.auties.whatsapp.protobuf.signal.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.protocol.CiphertextMessage;

import java.io.IOException;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage {
  private static final int SIGNATURE_LENGTH = 64;
  private static final int CURRENT_VERSION = 3;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] cipherText;
  
  private int version;
  
  private byte[] serialized;

  public SenderKeyMessage(int id, int iteration, byte[] ciphertext) {
    this(id, iteration, ciphertext, 0, null);
  }

  public SenderKeyMessage(byte[] serialized) {
    try {
      var serializedBinary = BinaryArray.of(serialized);
      var version = Byte.toUnsignedInt(serializedBinary.at(0)) >> 4;
      var message = serializedBinary.slice(1, serialized.length - 1 - SIGNATURE_LENGTH).data();
      var signature = serializedBinary.slice(serialized.length - 1 - SIGNATURE_LENGTH, SIGNATURE_LENGTH).data();
      Validate.isTrue(version > 3, "Legacy version");
      Validate.isTrue(version <= CURRENT_VERSION, "Unknown version");
      var senderKeyMessage = ProtobufDecoder.forType(getClass()).decode(message);

      this.serialized = serialized;
      this.version = version;
      this.id = senderKeyMessage.id();
      this.iteration = senderKeyMessage.iteration();
      this.cipherText = senderKeyMessage.cipherText();
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
