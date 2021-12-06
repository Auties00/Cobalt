package it.auties.whatsapp.protobuf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Cipher;
import it.auties.whatsapp.utils.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.util.ByteUtil;

import java.io.IOException;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyMessage {
  private static final int SIGNATURE_LENGTH = 64;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int id;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] ciphertext;

  private byte[] serialized;

  private int version;

  public SenderKeyMessage(byte[] serialized) {
    try {
      var serializedBinary = BinaryArray.of(serialized);
      var version = Byte.toUnsignedInt(serializedBinary.at(0)) >> 4;
      var message = serializedBinary.slice(1, serialized.length - 1 - SIGNATURE_LENGTH).data();
      var signature = serializedBinary.slice(serialized.length - 1 - SIGNATURE_LENGTH, SIGNATURE_LENGTH).data();
      Validate.isTrue(version > 3, "Legacy version");
      Validate.isTrue(version <= CipherConstants.CURRENT_VERSION, "Unknown version");
      var senderKeyMessage = ProtobufDecoder.forType(getClass()).decode(message);

      this.serialized = serialized;
      this.version = version;
      this.id = senderKeyMessage.id();
      this.iteration = senderKeyMessage.iteration();
      this.ciphertext = senderKeyMessage.ciphertext();
    } catch (IOException exception) {
      throw new RuntimeException("Cannot decode SenderKeyMessage", exception);
    }
  }

  public void verifySignature(byte[] signatureKey) {
    var signatureKeyBinary = BinaryArray.of(signatureKey);
    var message = signatureKeyBinary.slice(serialized.length - SIGNATURE_LENGTH);
    var signature = signatureKeyBinary.slice(serialized.length - SIGNATURE_LENGTH, SIGNATURE_LENGTH);
    Validate.isTrue(Cipher.verifySignature(signatureKey, message.data(), signature.data()), "Invalid signature");
  }

  public int type() {
    return CiphertextMessage.SENDERKEY_TYPE;
  }
}
