package com.github.auties00.cobalt.wire.linked.sync.data;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionData;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Plaintext record supplied by the primary device inside a snapshot recovery
 * response.
 *
 * <p>Unlike {@link SyncdRecord}, which stores its value as an encrypted blob,
 * a plaintext record exposes the already-decoded {@link SyncActionData}
 * together with the identifier of the key that was originally used to encrypt
 * it and the value MAC produced during that encryption. The companion
 * receiving a recovery re-derives the index MAC from the plaintext, reuses
 * the supplied value MAC for its sync-action entry store, and installs the
 * action directly without another encryption round-trip.
 */
@ProtobufMessage(name = "SyncdPlainTextRecord")
public final class SyncdPlainTextRecord {
    /**
     * Decoded sync action data (index, value and version).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncActionData value;

    /**
     * Identifier of the sync key that encrypted the original record on the
     * primary device.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] keyId;

    /**
     * Value MAC that was produced when the original record was encrypted.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mac;

    /**
     * Constructs a new plaintext record.
     *
     * @param value the decoded action data
     * @param keyId the original encryption key identifier
     * @param mac the original value MAC
     */
    SyncdPlainTextRecord(SyncActionData value, byte[] keyId, byte[] mac) {
        this.value = value;
        this.keyId = keyId;
        this.mac = mac;
    }

    /**
     * Returns the decoded action data of this record.
     *
     * @return the action data, or empty if absent
     */
    public Optional<SyncActionData> value() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the identifier of the sync key that encrypted the original
     * record. The companion uses this id to look up the key material when
     * re-deriving the index MAC.
     *
     * @return the key id bytes, or empty if absent
     */
    public Optional<byte[]> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Returns the original value MAC. The companion reuses it in its
     * sync-action entry store so that subsequent incremental LT-Hash
     * computations remain consistent.
     *
     * @return the value MAC bytes, or empty if absent
     */
    public Optional<byte[]> mac() {
        return Optional.ofNullable(mac);
    }

    /**
     * Sets the decoded action data.
     *
     * @param value the action data
     */
    public void setValue(SyncActionData value) {
        this.value = value;
    }

    /**
     * Sets the original encryption key identifier.
     *
     * @param keyId the key id bytes
     */
    public void setKeyId(byte[] keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the original value MAC.
     *
     * @param mac the value MAC bytes
     */
    public void setMac(byte[] mac) {
        this.mac = mac;
    }
}
