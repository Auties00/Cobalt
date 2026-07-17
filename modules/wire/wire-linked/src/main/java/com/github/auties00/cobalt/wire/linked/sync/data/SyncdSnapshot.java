package com.github.auties00.cobalt.wire.linked.sync.data;

import com.github.auties00.cobalt.wire.linked.signal.KeyId;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Authoritative snapshot of an app-state collection at a given version.
 *
 * <p>Snapshots are produced when the incremental patch history grows too
 * large or when a device joins an account and needs a starting point. They
 * carry the collection version, the full set of {@link SyncdRecord}
 * instances making up the current state, a MAC that authenticates the
 * snapshot as a whole, and the identifier of the sync key used for that MAC.
 * A receiving device installs the snapshot after verifying the MAC and then
 * keeps applying subsequent patches starting from the snapshot version.
 */
@ProtobufMessage(name = "SyncdSnapshot")
public final class SyncdSnapshot {
    /**
     * Version of the collection captured by the snapshot.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncdVersion version;

    /**
     * Full list of records making up the collection state.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<SyncdRecord> records;

    /**
     * MAC authenticating the snapshot as a whole.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mac;

    /**
     * Identifier of the sync key used to produce the snapshot MAC.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    KeyId keyId;


    /**
     * Constructs a new snapshot.
     *
     * @param version the collection version
     * @param records the records making up the collection state
     * @param mac the snapshot MAC
     * @param keyId the identifier of the sync key used for the MAC
     */
    SyncdSnapshot(SyncdVersion version, List<SyncdRecord> records, byte[] mac, KeyId keyId) {
        this.version = version;
        this.records = records;
        this.mac = mac;
        this.keyId = keyId;
    }

    /**
     * Returns the version of the snapshot.
     *
     * @return the snapshot version, or empty if absent
     */
    public Optional<SyncdVersion> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the records contained in the snapshot.
     *
     * @return an unmodifiable list of records, never {@code null}
     */
    public List<SyncdRecord> records() {
        return records == null ? List.of() : Collections.unmodifiableList(records);
    }

    /**
     * Returns the MAC authenticating the snapshot.
     *
     * @return the MAC bytes, or empty if absent
     */
    public Optional<byte[]> mac() {
        return Optional.ofNullable(mac);
    }

    /**
     * Returns the identifier of the key used to produce the MAC.
     *
     * @return the key id, or empty if absent
     */
    public Optional<KeyId> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Sets the snapshot version.
     *
     * @param version the collection version
     */
    public void setVersion(SyncdVersion version) {
        this.version = version;
    }

    /**
     * Sets the records contained in the snapshot.
     *
     * @param records the records
     */
    public void setRecords(List<SyncdRecord> records) {
        this.records = records;
    }

    /**
     * Sets the snapshot MAC.
     *
     * @param mac the MAC bytes
     */
    public void setMac(byte[] mac) {
        this.mac = mac;
    }

    /**
     * Sets the identifier of the key used to produce the MAC.
     *
     * @param keyId the key id
     */
    public void setKeyId(KeyId keyId) {
        this.keyId = keyId;
    }
}
