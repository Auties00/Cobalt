package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Snapshot recovery payload supplied by the primary device to rebuild a
 * companion's app-state collection.
 *
 * <p>When a companion fails to validate the MAC of a server snapshot (for
 * example after an irrecoverable key rotation) the primary device responds
 * with a recovery package containing an authoritative view of the collection.
 * The package carries the current collection version, the list of plaintext
 * mutation records that make up the collection, and the LT-Hash computed by
 * the primary over those records. The companion installs this state in place
 * of the corrupted snapshot so that subsequent patches apply consistently.
 *
 * <p>The protobuf bytes are typically gzip-compressed and embedded inside the
 * {@code collectionSnapshot} field of a snapshot-recovery IQ response.
 */
@ProtobufMessage(name = "SyncdSnapshotRecovery")
public final class SyncdSnapshotRecovery {
    /**
     * Collection version included in the recovery, used to align subsequent
     * incremental patches with the rebuilt state.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncdVersion version;

    /**
     * Name of the app-state collection that the recovery refers to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String collectionName;

    /**
     * Plaintext records that make up the current state of the collection.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<SyncdPlainTextRecord> mutationRecords;

    /**
     * LT-Hash over the mutation records, as computed by the primary device.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] collectionLthash;

    /**
     * Constructs a new recovery payload with all fields populated.
     *
     * @param version the collection version
     * @param collectionName the target collection name
     * @param mutationRecords the plaintext records making up the collection
     * @param collectionLthash the LT-Hash computed by the primary device
     */
    SyncdSnapshotRecovery(SyncdVersion version, String collectionName, List<SyncdPlainTextRecord> mutationRecords, byte[] collectionLthash) {
        this.version = version;
        this.collectionName = collectionName;
        this.mutationRecords = mutationRecords;
        this.collectionLthash = collectionLthash;
    }

    /**
     * Returns the collection version included in the recovery payload.
     *
     * @return the snapshot version, or empty if absent
     */
    public Optional<SyncdVersion> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the name of the collection that the recovery rebuilds.
     *
     * @return the collection name, or empty if absent
     */
    public Optional<String> collectionName() {
        return Optional.ofNullable(collectionName);
    }

    /**
     * Returns the plaintext mutation records that rebuild the collection.
     *
     * <p>Every record represents a {@code SET} operation: a recovery payload
     * contains the current state of the collection, not a list of historical
     * mutations.
     *
     * @return an unmodifiable list of plaintext records, never {@code null}
     */
    public List<SyncdPlainTextRecord> mutationRecords() {
        return mutationRecords == null ? List.of() : Collections.unmodifiableList(mutationRecords);
    }

    /**
     * Returns the LT-Hash computed by the primary device over the recovery
     * records. The companion installs this hash directly instead of
     * recomputing it from the rebuilt state.
     *
     * @return the LT-Hash bytes, or empty if absent
     */
    public Optional<byte[]> collectionLthash() {
        return Optional.ofNullable(collectionLthash);
    }

    /**
     * Sets the collection version.
     *
     * @param version the snapshot version
     */
    public void setVersion(SyncdVersion version) {
        this.version = version;
    }

    /**
     * Sets the collection name.
     *
     * @param collectionName the collection name
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the plaintext mutation records.
     *
     * @param mutationRecords the records to use as the rebuilt collection
     */
    public void setMutationRecords(List<SyncdPlainTextRecord> mutationRecords) {
        this.mutationRecords = mutationRecords;
    }

    /**
     * Sets the LT-Hash of the rebuilt collection.
     *
     * @param collectionLthash the LT-Hash bytes
     */
    public void setCollectionLthash(byte[] collectionLthash) {
        this.collectionLthash = collectionLthash;
    }
}
