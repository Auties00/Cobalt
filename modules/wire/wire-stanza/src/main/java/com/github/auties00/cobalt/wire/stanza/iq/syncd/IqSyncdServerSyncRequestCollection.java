package com.github.auties00.cobalt.wire.stanza.iq.syncd;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A single {@code <collection/>} entry inside an outbound
 * {@link IqSyncdServerSyncRequest}.
 *
 * <p>One entry is constructed per collection name (for example {@code "regular"},
 * {@code "regular_low"}, {@code "regular_high"}, {@code "critical_block"} or
 * {@code "critical_unblock_low"}) per sync iteration. A {@code null} version
 * requests an initial snapshot; a known version fetches only the patches above
 * it. Supplying {@code patch} bytes uploads encrypted local mutations as part of
 * the same iteration, while a {@code null} patch fetches only remote state.
 */
public final class IqSyncdServerSyncRequestCollection {
    /**
     * Holds the collection name, one of the values in WA Web's
     * {@code WASyncdConst.CollectionName}.
     */
    private final String name;

    /**
     * Holds the locally-known collection version, or {@code null} when the caller
     * has never synced this collection.
     */
    private final Long version;

    /**
     * Holds the encoded {@code SyncdPatch} protobuf carrying encrypted local
     * mutations to push, or {@code null} when this entry only fetches remote
     * state.
     */
    private final byte[] patch;

    /**
     * Constructs a new outbound collection entry.
     *
     * <p>The {@code patch} parameter is the already-encrypted-and-encoded protobuf
     * produced by the syncd request-build pipeline (LtHash computation, per-mutation
     * encryption, snapshot or patch MAC computation, and {@code SyncdPatch}
     * encoding); it is {@code null} when the caller has no local mutations to ship.
     *
     * @param name    the collection name; never {@code null}
     * @param version the locally-known version, or {@code null} when the caller
     *                has never synced this collection
     * @param patch   the encoded {@code SyncdPatch} bytes, or {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public IqSyncdServerSyncRequestCollection(String name, Long version, byte[] patch) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.version = version;
        this.patch = patch;
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
     * Returns the locally-known collection version.
     *
     * @return an {@link Optional} containing the version, or empty when the caller
     *         has never synced this collection
     */
    public Optional<Long> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the encoded {@code SyncdPatch} bytes carrying local mutations.
     *
     * @return an {@link Optional} containing the patch bytes, or empty when this
     *         entry only fetches remote state
     */
    public Optional<byte[]> patch() {
        return Optional.ofNullable(patch);
    }

    /**
     * Compares this entry to another for equality across name, version and patch
     * bytes.
     *
     * @param obj the object to compare against, or {@code null}
     * @return {@code true} when {@code obj} is an
     *         {@link IqSyncdServerSyncRequestCollection} with equal name, version
     *         and patch content
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSyncdServerSyncRequestCollection) obj;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.version, that.version)
                && Arrays.equals(this.patch, that.patch);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)} over name,
     * version and patch bytes.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, version, Arrays.hashCode(patch));
    }

    /**
     * Returns a debugging representation that elides the patch bytes to their
     * length.
     *
     * @return a string of the form
     *         {@code IqSyncdServerSyncRequestCollection[name=..., version=..., patch=byte[N]]}
     */
    @Override
    public String toString() {
        return "IqSyncdServerSyncRequestCollection[name=" + name
                + ", version=" + version
                + ", patch=" + (patch == null ? "null" : "byte[" + patch.length + "]") + ']';
    }
}
