package com.github.auties00.cobalt.wire.linked.device;

import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cryptographic proof that a message sender saw a particular snapshot of both
 * the sender's and the recipient's device lists at the moment the message was
 * encrypted.
 *
 * <p>WhatsApp's end to end encryption is multi device: every user may have a
 * primary phone plus several companion devices, and each device has its own
 * Signal identity key. When a client sends a message it must encrypt it
 * separately for every device in the conversation. To let the recipient
 * detect that a sender silently dropped or added a device (which could be a
 * sign of a man in the middle attack), WhatsApp attaches this metadata to
 * every encrypted payload.
 *
 * <p>The metadata contains a truncated hash of the sender's view of its own
 * device list together with the sender's view of the recipient's device list,
 * plus the timestamps at which those views were observed and the key indexes
 * of the devices that contributed to each hash. When the recipient receives
 * the message it recomputes the hashes using its own knowledge of the device
 * lists and compares them against these values. A mismatch signals that the
 * sender and the recipient disagree about who is supposed to be receiving the
 * message and triggers a device list re synchronisation or a security
 * notification shown to the user.
 *
 * <p>Both hashes are optional: the sender may omit them if it has never
 * observed a device list for the corresponding party. Both account type
 * fields default to {@link ADVEncryptionType#E2EE} when absent.
 *
 * @see com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities
 * @see ADVEncryptionType
 */
@ProtobufMessage(name = "DeviceListMetadata")
public final class DeviceListMetadata {
    /**
     * Truncated SHA 256 hash of the identity keys of the sender's own
     * companion devices, as seen by the sender when the message was
     * encrypted. Optional: may be {@code null} if the sender chose not to
     * include a self hash.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] senderKeyHash;

    /**
     * Timestamp at which the sender last observed its own device list. Used
     * by the recipient to distinguish a stale hash from a fresh one when
     * resolving a device list mismatch. Optional.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    Instant senderTimestamp;

    /**
     * Ordered list of device identifier indexes that were included in
     * {@link #senderKeyHash} when it was computed. Allows the recipient to
     * know exactly which devices the sender believed existed on its own side.
     * Empty if no self hash is present.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32, packed = true)
    List<Integer> senderKeyIndexes;

    /**
     * Encryption scheme used by the sender's account at the time the message
     * was sent. Defaults to {@link ADVEncryptionType#E2EE} when absent and is
     * used to distinguish end to end encrypted accounts from hosted or
     * interoperability accounts with a different protection model.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    ADVEncryptionType senderAccountType;

    /**
     * Encryption scheme of the recipient's account as understood by the
     * sender when the message was sent. Defaults to
     * {@link ADVEncryptionType#E2EE} when absent.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ADVEncryptionType receiverAccountType;

    /**
     * Truncated SHA 256 hash of the identity keys of the recipient's
     * companion devices, as seen by the sender when the message was
     * encrypted. Allows the recipient to check that the sender encrypted for
     * the same set of devices the recipient currently has. Optional.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] recipientKeyHash;

    /**
     * Timestamp at which the sender last observed the recipient's device
     * list. Used by the recipient together with its local timestamp to
     * decide which side holds the fresher view of the list. Optional.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    Instant recipientTimestamp;

    /**
     * Ordered list of device identifier indexes of the recipient that were
     * included in {@link #recipientKeyHash}. Empty if no recipient hash is
     * present.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32, packed = true)
    List<Integer> recipientKeyIndexes;


    /**
     * Creates a new device list metadata payload. This constructor is
     * package private and is reserved for the protobuf deserialiser and the
     * generated builder.
     *
     * @param senderKeyHash        the sender's own device key hash, or {@code null}
     * @param senderTimestamp      when the sender last observed its own device list, or {@code null}
     * @param senderKeyIndexes     the device indexes behind the sender hash, or {@code null}
     * @param senderAccountType    the sender's account encryption type, or {@code null}
     * @param receiverAccountType  the recipient's account encryption type as seen by the sender, or {@code null}
     * @param recipientKeyHash     the recipient's device key hash as seen by the sender, or {@code null}
     * @param recipientTimestamp   when the sender last observed the recipient's device list, or {@code null}
     * @param recipientKeyIndexes  the device indexes behind the recipient hash, or {@code null}
     */
    DeviceListMetadata(byte[] senderKeyHash, Instant senderTimestamp, List<Integer> senderKeyIndexes, ADVEncryptionType senderAccountType, ADVEncryptionType receiverAccountType, byte[] recipientKeyHash, Instant recipientTimestamp, List<Integer> recipientKeyIndexes) {
        this.senderKeyHash = senderKeyHash;
        this.senderTimestamp = senderTimestamp;
        this.senderKeyIndexes = senderKeyIndexes;
        this.senderAccountType = senderAccountType;
        this.receiverAccountType = receiverAccountType;
        this.recipientKeyHash = recipientKeyHash;
        this.recipientTimestamp = recipientTimestamp;
        this.recipientKeyIndexes = recipientKeyIndexes;
    }

    /**
     * Returns the truncated hash of the sender's own device identity keys
     * captured when the message was encrypted.
     *
     * @return the sender side key hash, or {@code Optional.empty()} if the
     *         sender did not include a self hash
     */
    public Optional<byte[]> senderKeyHash() {
        return Optional.ofNullable(senderKeyHash);
    }

    /**
     * Returns the moment at which the sender last observed its own device
     * list before computing {@link #senderKeyHash()}.
     *
     * @return the sender side observation timestamp, or {@code Optional.empty()}
     *         if no self hash is present
     */
    public Optional<Instant> senderTimestamp() {
        return Optional.ofNullable(senderTimestamp);
    }

    /**
     * Returns the unmodifiable list of the sender's own device indexes that
     * contributed to {@link #senderKeyHash()}.
     *
     * @return the sender side device indexes, or an empty list if no self
     *         hash is present
     */
    public List<Integer> senderKeyIndexes() {
        return senderKeyIndexes == null ? List.of() : Collections.unmodifiableList(senderKeyIndexes);
    }

    /**
     * Returns the encryption scheme that the sender's account was using when
     * the message was sent.
     *
     * @return the sender's account encryption type, or {@code Optional.empty()}
     *         if the field was omitted on the wire (in which case the implicit
     *         default {@link ADVEncryptionType#E2EE} applies)
     */
    public Optional<ADVEncryptionType> senderAccountType() {
        return Optional.ofNullable(senderAccountType);
    }

    /**
     * Returns the encryption scheme that the sender believed the recipient
     * was using when the message was sent.
     *
     * @return the recipient's account encryption type as seen by the sender,
     *         or {@code Optional.empty()} if the field was omitted on the
     *         wire (in which case the implicit default
     *         {@link ADVEncryptionType#E2EE} applies)
     */
    public Optional<ADVEncryptionType> receiverAccountType() {
        return Optional.ofNullable(receiverAccountType);
    }

    /**
     * Returns the truncated hash of the recipient's device identity keys as
     * observed by the sender when the message was encrypted.
     *
     * @return the recipient side key hash, or {@code Optional.empty()} if
     *         the sender did not include a recipient hash
     */
    public Optional<byte[]> recipientKeyHash() {
        return Optional.ofNullable(recipientKeyHash);
    }

    /**
     * Returns the moment at which the sender last observed the recipient's
     * device list before computing {@link #recipientKeyHash()}.
     *
     * @return the recipient side observation timestamp, or
     *         {@code Optional.empty()} if no recipient hash is present
     */
    public Optional<Instant> recipientTimestamp() {
        return Optional.ofNullable(recipientTimestamp);
    }

    /**
     * Returns the unmodifiable list of the recipient's device indexes that
     * contributed to {@link #recipientKeyHash()}.
     *
     * @return the recipient side device indexes, or an empty list if no
     *         recipient hash is present
     */
    public List<Integer> recipientKeyIndexes() {
        return recipientKeyIndexes == null ? List.of() : Collections.unmodifiableList(recipientKeyIndexes);
    }

    /**
     * Overrides the sender side key hash.
     *
     * @param senderKeyHash the new hash value, or {@code null} to clear it
     */
    public void setSenderKeyHash(byte[] senderKeyHash) {
        this.senderKeyHash = senderKeyHash;
    }

    /**
     * Overrides the sender side observation timestamp.
     *
     * @param senderTimestamp the new timestamp, or {@code null} to clear it
     */
    public void setSenderTimestamp(Instant senderTimestamp) {
        this.senderTimestamp = senderTimestamp;
    }

    /**
     * Overrides the sender side device indexes.
     *
     * @param senderKeyIndexes the new list of indexes, or {@code null} to
     *                         clear it
     */
    public void setSenderKeyIndexes(List<Integer> senderKeyIndexes) {
        this.senderKeyIndexes = senderKeyIndexes;
    }

    /**
     * Overrides the sender's own account encryption type.
     *
     * @param senderAccountType the new encryption type, or {@code null} to
     *                          clear it (the wire default
     *                          {@link ADVEncryptionType#E2EE} will then apply)
     */
    public void setSenderAccountType(ADVEncryptionType senderAccountType) {
        this.senderAccountType = senderAccountType;
    }

    /**
     * Overrides the recipient's account encryption type as seen by the sender.
     *
     * @param receiverAccountType the new encryption type, or {@code null} to
     *                            clear it (the wire default
     *                            {@link ADVEncryptionType#E2EE} will then apply)
     */
    public void setReceiverAccountType(ADVEncryptionType receiverAccountType) {
        this.receiverAccountType = receiverAccountType;
    }

    /**
     * Overrides the recipient side key hash.
     *
     * @param recipientKeyHash the new hash value, or {@code null} to clear it
     */
    public void setRecipientKeyHash(byte[] recipientKeyHash) {
        this.recipientKeyHash = recipientKeyHash;
    }

    /**
     * Overrides the recipient side observation timestamp.
     *
     * @param recipientTimestamp the new timestamp, or {@code null} to clear it
     */
    public void setRecipientTimestamp(Instant recipientTimestamp) {
        this.recipientTimestamp = recipientTimestamp;
    }

    /**
     * Overrides the recipient side device indexes.
     *
     * @param recipientKeyIndexes the new list of indexes, or {@code null} to
     *                            clear it
     */
    public void setRecipientKeyIndexes(List<Integer> recipientKeyIndexes) {
        this.recipientKeyIndexes = recipientKeyIndexes;
    }
}
