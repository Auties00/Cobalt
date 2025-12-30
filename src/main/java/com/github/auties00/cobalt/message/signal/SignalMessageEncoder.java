package com.github.auties00.cobalt.message.signal;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.model.MessageContainerSpec;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessageBuilder;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalPreKeyMessage;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.util.Arrays;
import java.util.Objects;

import static com.github.auties00.cobalt.message.signal.SignalMessageConstants.*;

/**
 * Encoder for WhatsApp messages using Signal Protocol encryption.
 * This is the counterpart to MessageDecoder for outgoing messages.
 */
public final class SignalMessageEncoder {
    private final SignalSessionCipher sessionCipher;
    private final SignalGroupCipher groupCipher;

    public SignalMessageEncoder(SignalSessionCipher sessionCipher, SignalGroupCipher groupCipher) {
        this.sessionCipher = sessionCipher;
        this.groupCipher = groupCipher;
    }

    /**
     * Encodes and encrypts a message for a 1:1 chat recipient.
     *
     * @param recipientAddress the Signal protocol address of the recipient
     * @param message          the message container to encrypt
     * @return the encryption result containing ciphertext and message type
     */
    public Result encode(SignalProtocolAddress recipientAddress, MessageContainer message) {
        Objects.requireNonNull(recipientAddress, "recipientAddress cannot be null");
        Objects.requireNonNull(message, "message cannot be null");

        var paddedPlaintext = encodeAndPad(message);
        var ciphertextMessage = sessionCipher.encrypt(recipientAddress, paddedPlaintext);

        var ciphertext = ciphertextMessage.toSerialized();
        var type = ciphertextMessage instanceof SignalPreKeyMessage ? PKMSG : MSG;

        return Result.ofSession(ciphertext, type);
    }

    /**
     * Encodes and encrypts a message for a 1:1 chat recipient using JID.
     *
     * @param recipientJid the JID of the recipient device
     * @param message      the message container to encrypt
     * @return the encryption result containing ciphertext and message type
     */
    public Result encode(Jid recipientJid, MessageContainer message) {
        Objects.requireNonNull(recipientJid, "recipientJid cannot be null");
        return encode(recipientJid.toSignalAddress(), message);
    }

    /**
     * Encodes and encrypts a message for a group chat using sender key encryption.
     *
     * @param groupJid     the JID of the group
     * @param senderDevice the JID of the sender's device
     * @param message      the message container to encrypt
     * @return the encryption result containing ciphertext and sender key bytes
     */
    public Result encodeForGroup(Jid groupJid, Jid senderDevice, MessageContainer message) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderDevice, "senderDevice cannot be null");
        Objects.requireNonNull(message, "message cannot be null");

        var senderKeyName = new SignalSenderKeyName(groupJid.toString(), senderDevice.toSignalAddress());
        var paddedPlaintext = encodeAndPad(message);

        // Ensure we have a sender key for this group
        var distributionMessage = groupCipher.create(senderKeyName);

        // Encrypt the message with sender key
        var ciphertextMessage = groupCipher.encrypt(senderKeyName, paddedPlaintext);
        var ciphertext = ciphertextMessage.toSerialized();

        return Result.ofSenderKey(ciphertext, distributionMessage.toSerialized());
    }

    /**
     * Creates a sender key distribution message for a group.
     * This is used to distribute the sender key to new group members or devices.
     *
     * @param groupJid     the JID of the group
     * @param senderDevice the JID of the sender's device
     * @return the sender key distribution message bytes
     */
    public byte[] createSenderKeyDistribution(Jid groupJid, Jid senderDevice) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderDevice, "senderDevice cannot be null");

        var senderKeyName = new SignalSenderKeyName(groupJid.toString(), senderDevice.toSignalAddress());
        var distributionMessage = groupCipher.create(senderKeyName);
        return distributionMessage.toSerialized();
    }

    /**
     * Creates a SenderKeyDistributionMessage model for embedding in a MessageContainer.
     *
     * @param groupJid     the JID of the group
     * @param senderDevice the JID of the sender's device
     * @return the sender key distribution message model
     */
    public SenderKeyDistributionMessage createSenderKeyDistributionMessage(Jid groupJid, Jid senderDevice) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderDevice, "senderDevice cannot be null");

        var senderKeyBytes = createSenderKeyDistribution(groupJid, senderDevice);
        return new SenderKeyDistributionMessageBuilder()
                .groupJid(groupJid)
                .data(senderKeyBytes)
                .build();
    }

    /**
     * Wraps a sender key distribution message in Signal session encryption for a specific recipient.
     * This is used to send the sender key to devices that need it.
     *
     * @param recipientAddress   the Signal protocol address of the recipient
     * @param groupJid           the JID of the group
     * @param senderDevice       the JID of the sender's device
     * @return the encryption result containing the wrapped sender key distribution
     */
    public Result wrapSenderKeyDistribution(
            SignalProtocolAddress recipientAddress,
            Jid groupJid,
            Jid senderDevice
    ) {
        Objects.requireNonNull(recipientAddress, "recipientAddress cannot be null");
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderDevice, "senderDevice cannot be null");

        // Create the sender key distribution message
        var distributionMessage = createSenderKeyDistributionMessage(groupJid, senderDevice);

        // Wrap it in a MessageContainer
        var container = MessageContainer.of(distributionMessage);

        // Encrypt with Signal session
        return encode(recipientAddress, container);
    }


    /**
     * Encodes a message container to protobuf and adds WhatsApp-specific padding.
     * The padding ensures the message length is aligned to BLOCK_SIZE (16 bytes).
     *
     * @param message the message container to encode
     * @return the padded plaintext bytes
     */
    private byte[] encodeAndPad(MessageContainer message) {
        // Calculate encoded length
        var encodedLength = MessageContainerSpec.sizeOf(message);

        // Calculate padding: align to block size, minimum 1 byte for padding length
        var paddingLength = BLOCK_SIZE - (encodedLength % BLOCK_SIZE);

        // Create result with padding
        var result = new byte[encodedLength + paddingLength];
        MessageContainerSpec.encode(message, ProtobufOutputStream.toBytes(result, 0));

        // Fill padding bytes with the padding length value (PKCS7-style)
        Arrays.fill(result, encodedLength, result.length, (byte) paddingLength);

        return result;
    }

    /**
     * Represents the result of message encryption.
     * Contains the encrypted ciphertext, the encryption type, and optionally the sender key bytes for group messages.
     */
    public record Result(
            byte[] ciphertext,
            String type,
            byte[] senderKeyBytes
    ) {
        /**
         * Creates an encryption result for a 1:1 message (MSG or PKMSG).
         *
         * @param ciphertext the encrypted message bytes
         * @param type       the encryption type (MSG or PKMSG)
         * @return the encryption result
         */
        public static Result ofSession(byte[] ciphertext, String type) {
            return new Result(
                    Objects.requireNonNull(ciphertext, "ciphertext cannot be null"),
                    Objects.requireNonNull(type, "type cannot be null"),
                    null
            );
        }

        /**
         * Creates an encryption result for a group message (SKMSG).
         *
         * @param ciphertext     the encrypted message bytes
         * @param senderKeyBytes the sender key distribution message bytes
         * @return the encryption result
         */
        public static Result ofSenderKey(byte[] ciphertext, byte[] senderKeyBytes) {
            return new Result(
                    Objects.requireNonNull(ciphertext, "ciphertext cannot be null"),
                    SKMSG,
                    senderKeyBytes
            );
        }

        /**
         * Returns whether this result contains a pre-key message.
         *
         * @return true if this is a PKMSG type
         */
        public boolean isPreKeyMessage() {
            return PKMSG.equals(type);
        }

        /**
         * Returns whether this result contains a sender key message.
         *
         * @return true if this is a SKMSG type
         */
        public boolean isSenderKeyMessage() {
            return SKMSG.equals(type);
        }

        /**
         * Returns whether this result has sender key bytes for distribution.
         *
         * @return true if sender key bytes are present
         */
        public boolean hasSenderKeyBytes() {
            return senderKeyBytes != null && senderKeyBytes.length > 0;
        }
    }
}
