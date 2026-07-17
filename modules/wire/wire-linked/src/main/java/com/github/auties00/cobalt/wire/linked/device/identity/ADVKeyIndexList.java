package com.github.auties00.cobalt.wire.linked.device.identity;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Enumerates which companion device slots are currently valid for an account.
 *
 * <p>A WhatsApp account can have several companion devices paired to it simultaneously
 * (Web, Desktop, tablets, etc.). Each companion occupies a numbered slot referred to as a
 * key index. When a device is linked or unlinked, the account rebuilds this list so that
 * peers can tell which slots still host live devices and which have been retired.
 *
 * <p>This structure is never sent on its own. It is serialized into the {@code details}
 * field of a {@link ADVSignedKeyIndexList} and signed by the account so that peers can
 * trust the membership of the list without contacting the server. The signed blob is
 * typically distributed alongside outgoing messages as part of the authenticated device
 * verification handshake.
 *
 * <p>Instances are mutable and expose both {@link Optional}-style getters and plain
 * setters, so the same object can be decoded, patched, and re-encoded during pairing and
 * key rotation flows.
 */
@ProtobufMessage(name = "ADVKeyIndexList")
public final class ADVKeyIndexList {
    /**
     * The raw identifier of the account that owns this list.
     *
     * <p>Optional on the wire. Serialized as a protobuf {@code uint32}. When present it
     * matches the device id of the primary account device, so recipients can tie the list
     * back to a specific user.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer rawId;

    /**
     * When the list was last regenerated.
     *
     * <p>Optional on the wire. Transmitted as a protobuf {@code uint64} of seconds
     * since the Unix epoch and converted to a Java {@link Instant} via
     * {@link InstantSecondsMixin}. Peers use this timestamp to discard obsolete lists and
     * to trigger re-verification when the stored copy becomes too old.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * The most recent key index allocated by the account.
     *
     * <p>Optional on the wire. Acts as a monotonically increasing counter: it is bumped
     * whenever a new companion slot is provisioned, even if the slot is later retired.
     * Combined with {@link #validIndexes} it lets clients distinguish "never existed"
     * slots from "exists but deleted" slots.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer currentIndex;

    /**
     * The set of key indexes that currently correspond to live companion devices.
     *
     * <p>Serialized as a packed repeated {@code uint32}. Every companion whose entry is
     * present in this list is considered authorised; any companion whose index is missing
     * has been unlinked and must no longer be trusted. The list is unordered and may be
     * empty when no companions are paired.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32, packed = true)
    List<Integer> validIndexes;

    /**
     * The encryption scheme used by the owning account.
     *
     * <p>Optional on the wire, defaults to {@link ADVEncryptionType#E2EE} when absent.
     * Controls which signature prefix is used when the account signs the serialized
     * representation of this list.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ADVEncryptionType accountType;

    /**
     * Constructs a key index list with the given properties. Package-private: instances
     * are created through the generated {@code ADVKeyIndexListBuilder} or by the protobuf
     * decoder.
     *
     * @param rawId         the raw account identifier, or {@code null} if unknown
     * @param timestamp     when the list was generated, or {@code null} if unknown
     * @param currentIndex  the highest allocated key index, or {@code null} if unknown
     * @param validIndexes  the indexes of currently authorised companions, or {@code null}
     * @param accountType   the encryption scheme of the account, or {@code null} for the default
     */
    ADVKeyIndexList(Integer rawId, Instant timestamp, Integer currentIndex, List<Integer> validIndexes, ADVEncryptionType accountType) {
        this.rawId = rawId;
        this.timestamp = timestamp;
        this.currentIndex = currentIndex;
        this.validIndexes = validIndexes;
        this.accountType = accountType;
    }

    /**
     * Returns the raw identifier of the account that owns this list.
     *
     * @return the raw account id, or {@link OptionalInt#empty()} if the field was absent
     */
    public OptionalInt rawId() {
        return rawId == null ? OptionalInt.empty() : OptionalInt.of(rawId);
    }

    /**
     * Returns when this list was regenerated.
     *
     * @return the timestamp, or {@link Optional#empty()} if the field was absent
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the highest key index ever allocated by the account.
     *
     * @return the current index, or {@link OptionalInt#empty()} if the field was absent
     */
    public OptionalInt currentIndex() {
        return currentIndex == null ? OptionalInt.empty() : OptionalInt.of(currentIndex);
    }

    /**
     * Returns the indexes of companion slots that are currently authorised.
     *
     * <p>The returned list is an unmodifiable view. An empty list means no companions are
     * paired or the field was omitted entirely; both situations are represented the same
     * way so that callers do not have to distinguish them.
     *
     * @return an unmodifiable list of valid indexes, never {@code null}
     */
    public List<Integer> validIndexes() {
        return validIndexes == null ? List.of() : Collections.unmodifiableList(validIndexes);
    }

    /**
     * Returns the encryption scheme used by the owning account.
     *
     * @return the account encryption type, or {@link Optional#empty()} when the caller
     *         should treat the value as {@link ADVEncryptionType#E2EE}
     */
    public Optional<ADVEncryptionType> accountType() {
        return Optional.ofNullable(accountType);
    }

    /**
     * Sets the raw account identifier.
     *
     * @param rawId the new identifier, or {@code null} to clear
     */
    public void setRawId(Integer rawId) {
        this.rawId = rawId;
    }

    /**
     * Sets the generation timestamp.
     *
     * @param timestamp the new timestamp, or {@code null} to clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the highest allocated key index.
     *
     * @param currentIndex the new current index, or {@code null} to clear
     */
    public void setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
    }

    /**
     * Replaces the set of currently authorised companion indexes.
     *
     * <p>The list is stored by reference; callers should avoid mutating it after calling
     * this setter because the instance is treated as owned by the message.
     *
     * @param validIndexes the new list of valid indexes, or {@code null} to clear
     */
    public void setValidIndexes(List<Integer> validIndexes) {
        this.validIndexes = validIndexes;
    }

    /**
     * Sets the encryption scheme of the owning account.
     *
     * @param accountType the new account encryption type, or {@code null} to use the default
     */
    public void setAccountType(ADVEncryptionType accountType) {
        this.accountType = accountType;
    }
}
