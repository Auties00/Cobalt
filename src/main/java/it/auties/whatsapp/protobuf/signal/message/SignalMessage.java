package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.*;
import lombok.experimental.Accessors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class SignalMessage implements SignalProtocolMessage {
    private static final int CURRENT_VERSION = 3;
    private static final int MAC_LENGTH = 8;
    private static final String HMAC = "HmacSHA256";

    private int messageVersion;

    @JsonProperty(value = "1")
    @JsonPropertyDescription("bytes")
    private byte[] ratchetKey;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("uint32")
    private int counter;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("bytes")
    private byte[] ciphertext;

    private byte[] serialized;

    public static boolean isLegacy(byte[] message) {
        return message != null
                && message.length >= 1
                && Byte.toUnsignedInt(message[0]) >> 4 != CURRENT_VERSION;
    }

    public SignalMessage(byte[] serialized) {
        try {
            var deserialized = ProtobufDecoder.forType(getClass()).decode(serialized);
            this.serialized = serialized;
            this.ratchetKey = Arrays.copyOfRange(deserialized.ratchetKey(), 1, deserialized.ratchetKey().length);
            this.messageVersion = Byte.toUnsignedInt(serialized[0]) >> 4;
            this.counter = deserialized.counter();
            this.previousCounter = deserialized.previousCounter();
            this.ciphertext = deserialized.ciphertext();
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode SignalMessage", exception);
        }
    }

    public SignalMessage(int messageVersion, SecretKeySpec macKey, byte[] senderRatchetKey, int counter, int previousCounter, byte[] ciphertext, byte[] senderIdentityKey, byte[] receiverIdentityKey) {
        var version = (byte) (messageVersion << 4 | CURRENT_VERSION);
        var message = ProtobufEncoder.encode(this);
        var mac = getMac(senderIdentityKey, receiverIdentityKey, macKey, BinaryArray.of(version).append(message).data());
        this.serialized = BinaryArray.of(version).append(message).append(mac).data();
        this.ratchetKey = senderRatchetKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        this.messageVersion = messageVersion;
    }

    public boolean verifyMac(byte[] senderIdentityKey, byte[] receiverIdentityKey, SecretKeySpec macKey) {
        var binary = BinaryArray.of(serialized);
        var divider = serialized.length - MAC_LENGTH;
        var ourMac = getMac(senderIdentityKey, receiverIdentityKey, macKey, binary.cut(divider).data());
        return Arrays.equals(ourMac, binary.slice(divider).data());
    }

    @SneakyThrows
    private byte[] getMac(byte[] senderIdentityKey, byte[] receiverIdentityKey, SecretKeySpec macKey, byte[] serialized) {
        var mac = Mac.getInstance(HMAC);
        mac.init(macKey);
        mac.update(senderIdentityKey);
        mac.update(receiverIdentityKey);
        return Arrays.copyOf(mac.doFinal(serialized), MAC_LENGTH);
    }
}
