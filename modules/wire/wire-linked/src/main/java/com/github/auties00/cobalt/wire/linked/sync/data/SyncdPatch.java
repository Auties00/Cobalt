package com.github.auties00.cobalt.wire.linked.sync.data;

import com.github.auties00.cobalt.wire.linked.error.DisconnectReason;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.signal.KeyId;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Incremental update to an app-state collection, produced by one device of an
 * account and replayed by every other device to stay consistent.
 *
 * <p>A patch bundles a version, an ordered list of {@link SyncdMutation}
 * entries (set/remove operations on records), a snapshot MAC and a patch MAC
 * that together authenticate the change, the identifier of the sync key used
 * to build the MACs, and the index of the device that generated the patch.
 * Large mutation sets are not embedded directly: instead an
 * {@link ExternalBlobReference} points at a media blob that decodes to a
 * {@link SyncdMutations} message. The optional {@link DisconnectReason}
 * carries a termination reason so that a peer that receives an exit-patch can
 * recover cleanly, and the optional {@link PatchDebugData} bytes provide
 * diagnostic context for MAC mismatches.
 */
@ProtobufMessage(name = "SyncdPatch")
public final class SyncdPatch {
    /**
     * Collection version resulting from applying this patch.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncdVersion version;

    /**
     * Inline mutations contained in the patch, when small enough to embed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<SyncdMutation> mutations;

    /**
     * Reference to an external media blob that, once downloaded and decoded,
     * provides the full {@link SyncdMutations} payload.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ExternalBlobReference externalMutations;

    /**
     * Snapshot MAC covering the collection state produced by the patch.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] snapshotMac;

    /**
     * Patch MAC covering the mutations contained in this patch.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] patchMac;

    /**
     * Identifier of the sync key used to produce the MACs.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    KeyId keyId;

    /**
     * Optional disconnect reason attached to exit patches.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    DisconnectReason exitCode;

    /**
     * Index of the device that produced the patch.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT32)
    Integer deviceIndex;

    /**
     * Encoded {@link PatchDebugData} bytes carrying diagnostic information.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] clientDebugData;


    /**
     * Constructs a new patch with all fields populated.
     *
     * @param version the resulting collection version
     * @param mutations the inline mutations
     * @param externalMutations the reference to external mutations, if any
     * @param snapshotMac the snapshot MAC
     * @param patchMac the patch MAC
     * @param keyId the identifier of the sync key used for MACs
     * @param exitCode the optional disconnect reason
     * @param deviceIndex the index of the producing device
     * @param clientDebugData the encoded debug payload
     */
    SyncdPatch(SyncdVersion version, List<SyncdMutation> mutations, ExternalBlobReference externalMutations, byte[] snapshotMac, byte[] patchMac, KeyId keyId, DisconnectReason exitCode, Integer deviceIndex, byte[] clientDebugData) {
        this.version = version;
        this.mutations = mutations;
        this.externalMutations = externalMutations;
        this.snapshotMac = snapshotMac;
        this.patchMac = patchMac;
        this.keyId = keyId;
        this.exitCode = exitCode;
        this.deviceIndex = deviceIndex;
        this.clientDebugData = clientDebugData;
    }

    /**
     * Returns the collection version produced by applying this patch.
     *
     * @return the version, or empty if absent
     */
    public Optional<SyncdVersion> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the inline mutations contained in this patch.
     *
     * @return an unmodifiable list of mutations, never {@code null}
     */
    public List<SyncdMutation> mutations() {
        return mutations == null ? List.of() : Collections.unmodifiableList(mutations);
    }

    /**
     * Returns the reference to external mutations, when the mutation set is
     * too large to embed directly.
     *
     * @return the external mutation reference, or empty if inlined
     */
    public Optional<ExternalBlobReference> externalMutations() {
        return Optional.ofNullable(externalMutations);
    }

    /**
     * Returns the snapshot MAC of the state produced by the patch.
     *
     * @return the snapshot MAC bytes, or empty if absent
     */
    public Optional<byte[]> snapshotMac() {
        return Optional.ofNullable(snapshotMac);
    }

    /**
     * Returns the MAC covering the mutations contained in the patch.
     *
     * @return the patch MAC bytes, or empty if absent
     */
    public Optional<byte[]> patchMac() {
        return Optional.ofNullable(patchMac);
    }

    /**
     * Returns the identifier of the sync key used to produce the MACs.
     *
     * @return the key id, or empty if absent
     */
    public Optional<KeyId> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Returns the disconnect reason attached to an exit patch.
     *
     * @return the disconnect reason, or empty if this is not an exit patch
     */
    public Optional<DisconnectReason> exitCode() {
        return Optional.ofNullable(exitCode);
    }

    /**
     * Returns the index of the device that produced the patch.
     *
     * @return the device index, or empty if absent
     */
    public OptionalInt deviceIndex() {
        return deviceIndex == null ? OptionalInt.empty() : OptionalInt.of(deviceIndex);
    }

    /**
     * Returns the raw encoded debug data bytes, before any decoding.
     *
     * @return the debug bytes, or empty if absent
     */
    public Optional<byte[]> clientDebugData() {
        return Optional.ofNullable(clientDebugData);
    }

    /**
     * Decodes {@link #clientDebugData()} into a {@link PatchDebugData}.
     *
     * <p>The debug bytes are intended for diagnostic logging on the receiving
     * side and must never interfere with sync application. As a result any
     * decoding failure, malformed payload or missing data is swallowed and
     * surfaced as {@link Optional#empty()} rather than a thrown exception.
     *
     * @return the decoded debug payload, or empty if absent or undecodable
     */
    public Optional<PatchDebugData> decodedClientDebugData() {
        if (clientDebugData == null || clientDebugData.length == 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(PatchDebugDataSpec.decode(clientDebugData));
        } catch (Exception e) {
            // Debug data failure must not break sync - swallow and return empty.
            return Optional.empty();
        }
    }

    /**
     * Sets the collection version resulting from this patch.
     *
     * @param version the collection version
     */
    public void setVersion(SyncdVersion version) {
        this.version = version;
    }

    /**
     * Sets the inline mutations.
     *
     * @param mutations the ordered mutations
     */
    public void setMutations(List<SyncdMutation> mutations) {
        this.mutations = mutations;
    }

    /**
     * Sets the reference to external mutations.
     *
     * @param externalMutations the external mutation reference
     */
    public void setExternalMutations(ExternalBlobReference externalMutations) {
        this.externalMutations = externalMutations;
    }

    /**
     * Sets the snapshot MAC.
     *
     * @param snapshotMac the MAC bytes
     */
    public void setSnapshotMac(byte[] snapshotMac) {
        this.snapshotMac = snapshotMac;
    }

    /**
     * Sets the patch MAC.
     *
     * @param patchMac the MAC bytes
     */
    public void setPatchMac(byte[] patchMac) {
        this.patchMac = patchMac;
    }

    /**
     * Sets the identifier of the sync key used to produce the MACs.
     *
     * @param keyId the key id
     */
    public void setKeyId(KeyId keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the disconnect reason for an exit patch.
     *
     * @param exitCode the disconnect reason
     */
    public void setExitCode(DisconnectReason exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Sets the index of the producing device.
     *
     * @param deviceIndex the device index
     */
    public void setDeviceIndex(Integer deviceIndex) {
        this.deviceIndex = deviceIndex;
    }

    /**
     * Sets the raw encoded debug data bytes.
     *
     * @param clientDebugData the debug bytes
     */
    public void setClientDebugData(byte[] clientDebugData) {
        this.clientDebugData = clientDebugData;
    }
}
