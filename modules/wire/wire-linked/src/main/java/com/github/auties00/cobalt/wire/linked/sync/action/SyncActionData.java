package com.github.auties00.cobalt.wire.linked.sync.action;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Wire level envelope for a single app state sync mutation before it is
 * encrypted and included in a patch.
 *
 * <p>Each mutation carries the raw index bytes that deterministically
 * identify the mutation key, the decoded {@link SyncActionValue} payload,
 * a random padding block used to prevent length analysis on the encrypted
 * blob, and the action schema version. Senders populate all four fields;
 * receivers read them back from decrypted patches and forward them to the
 * appropriate handler.
 */
@ProtobufMessage(name = "SyncActionData")
public final class SyncActionData {
    /**
     * The raw bytes of the mutation index, typically the UTF-8 encoding of
     * the JSON index array produced by
     * {@link SyncAction#toIndex(SyncActionArgs)}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] index;

    /**
     * The decoded payload of the mutation, containing the concrete action
     * and its typed fields.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncActionValue value;

    /**
     * A random padding block added before encryption to obscure the
     * plaintext length of the encoded value.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] padding;

    /**
     * The schema version declared by the originating action, used by
     * receivers to gate handlers that require a minimum version.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    Integer version;

    /**
     * Constructs a new sync action data envelope with the given fields.
     *
     * @param index the raw index bytes
     * @param value the decoded action payload
     * @param padding the random padding block
     * @param version the action schema version, or {@code null} if absent
     */
    SyncActionData(byte[] index, SyncActionValue value, byte[] padding, Integer version) {
        this.index = index;
        this.value = value;
        this.padding = padding;
        this.version = version;
    }

    /**
     * Returns the raw index bytes of the mutation.
     *
     * @return an optional containing the index bytes, empty if absent
     */
    public Optional<byte[]> index() {
        return Optional.ofNullable(index);
    }

    /**
     * Returns the decoded action payload.
     *
     * @return an optional containing the payload, empty if absent
     */
    public Optional<SyncActionValue> value() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the random padding block.
     *
     * @return an optional containing the padding bytes, empty if absent
     */
    public Optional<byte[]> padding() {
        return Optional.ofNullable(padding);
    }

    /**
     * Returns the action schema version.
     *
     * @return an optional containing the version, empty if absent
     */
    public OptionalInt version() {
        return version == null ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * Sets the raw index bytes of the mutation.
     *
     * @param index the index bytes
     */
    public void setIndex(byte[] index) {
        this.index = index;
    }

    /**
     * Sets the decoded action payload.
     *
     * @param value the action payload
     */
    public void setValue(SyncActionValue value) {
        this.value = value;
    }

    /**
     * Sets the random padding block.
     *
     * @param padding the padding bytes
     */
    public void setPadding(byte[] padding) {
        this.padding = padding;
    }

    /**
     * Sets the action schema version.
     *
     * @param version the action version, or {@code null} to clear it
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
