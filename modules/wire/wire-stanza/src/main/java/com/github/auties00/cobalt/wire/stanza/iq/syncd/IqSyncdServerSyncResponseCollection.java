package com.github.auties00.cobalt.wire.stanza.iq.syncd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * A single {@code <collection/>} projection inside an inbound
 * {@link IqSyncdServerSyncResponse.Success} reply.
 *
 * <p>Each entry carries the wire-derived {@link IqSyncdServerSyncCollectionState
 * state} (computed by the parser from the {@code type}, {@code <error code>} and
 * {@code has_more_patches} attributes), the relay-issued {@code version}, plus the
 * optional {@code <patches>} and {@code <snapshot>} payloads. The state-specific
 * transition drives the sync loop: {@code SUCCESS*} arms apply the payloads,
 * {@code CONFLICT*} arms reconcile, and {@code ERROR_*} arms drop or schedule a
 * retry.
 *
 * @implNote
 * This implementation surfaces both {@code <patches>} entries and the
 * {@code <snapshot>} body as raw byte arrays; the encrypted-patch decoding (LtHash
 * verification, per-mutation decryption) happens in the caller's apply pipeline
 * rather than at parse time. The {@code snapshot} bytes encode an
 * {@code ExternalBlobReference} protobuf pointing at the snapshot blob in MMS, and
 * the {@code patches} bytes encode raw {@code SyncdPatch} protobufs; both are
 * decoded at a higher layer.
 */
public final class IqSyncdServerSyncResponseCollection {
    /**
     * Holds the collection name, one of the values in WA Web's
     * {@code WASyncdConst.CollectionName}.
     */
    private final String name;

    /**
     * Holds the wire-derived collection state.
     */
    private final IqSyncdServerSyncCollectionState state;

    /**
     * Holds the relay-issued collection version, or {@code null} when the relay
     * omitted the attribute.
     */
    private final Long version;

    /**
     * Holds the encoded patch payloads returned in the {@code <patches/>} child,
     * one entry per {@code <patch/>} grandchild.
     */
    private final List<byte[]> patches;

    /**
     * Holds the encoded snapshot payload returned in the {@code <snapshot/>} child,
     * or {@code null} when absent.
     */
    private final byte[] snapshot;

    /**
     * Constructs a new inbound collection projection.
     *
     * @param name     the collection name; never {@code null}
     * @param state    the wire-derived state; never {@code null}
     * @param version  the relay-issued version, or {@code null}
     * @param patches  the encoded patch payloads; {@code null} is treated as an
     *                 empty list
     * @param snapshot the encoded snapshot payload, or {@code null}
     * @throws NullPointerException if {@code name} or {@code state} is {@code null}
     */
    public IqSyncdServerSyncResponseCollection(String name,
                                               IqSyncdServerSyncCollectionState state,
                                               Long version,
                                               List<byte[]> patches,
                                               byte[] snapshot) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.version = version;
        this.patches = patches == null ? List.of() : patches;
        this.snapshot = snapshot;
    }

    /**
     * Returns the collection name.
     *
     * @return the name; never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the wire-derived collection state.
     *
     * @return the state; never {@code null}
     */
    public IqSyncdServerSyncCollectionState state() {
        return state;
    }

    /**
     * Returns the relay-issued collection version.
     *
     * @return an {@link Optional} containing the version, or empty when absent
     */
    public Optional<Long> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the list of encoded patch payloads.
     *
     * @return an unmodifiable view of the patches; never {@code null}, possibly
     *         empty
     */
    public SequencedCollection<byte[]> patches() {
        return Collections.unmodifiableSequencedCollection(patches);
    }

    /**
     * Returns the encoded snapshot payload.
     *
     * @return an {@link Optional} containing the snapshot bytes, or empty when
     *         absent
     */
    public Optional<byte[]> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    /**
     * Compares this projection to another for equality across name, state, version,
     * patches and snapshot bytes.
     *
     * @param obj the object to compare against, or {@code null}
     * @return {@code true} when {@code obj} is an
     *         {@link IqSyncdServerSyncResponseCollection} equal in every field
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSyncdServerSyncResponseCollection) obj;
        return Objects.equals(this.name, that.name)
                && this.state == that.state
                && Objects.equals(this.version, that.version)
                && Objects.equals(this.patches, that.patches)
                && Arrays.equals(this.snapshot, that.snapshot);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)} over name, state,
     * version, patches and snapshot bytes.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, state, version, patches, Arrays.hashCode(snapshot));
    }

    /**
     * Returns a debugging representation that elides the patch and snapshot bytes to
     * their counts and length.
     *
     * @return a string of the form
     *         {@code IqSyncdServerSyncResponseCollection[name=..., state=..., version=..., patches=N, snapshot=byte[M]]}
     */
    @Override
    public String toString() {
        return "IqSyncdServerSyncResponseCollection[name=" + name
                + ", state=" + state
                + ", version=" + version
                + ", patches=" + patches.size()
                + ", snapshot=" + (snapshot == null ? "null" : "byte[" + snapshot.length + "]") + ']';
    }
}
