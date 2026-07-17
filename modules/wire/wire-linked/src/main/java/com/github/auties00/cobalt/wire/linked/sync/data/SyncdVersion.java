package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalLong;

/**
 * Monotonic counter identifying a point in an app-state collection's history.
 *
 * <p>Every patch, snapshot and recovery payload is tagged with a version.
 * Devices use these values to detect gaps, order patches and validate that a
 * snapshot corresponds to the expected state. The counter is stored as an
 * unsigned 64-bit integer on the wire.
 */
@ProtobufMessage(name = "SyncdVersion")
public final class SyncdVersion {
    /**
     * Raw unsigned 64-bit version counter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    Long version;


    /**
     * Constructs a new version wrapper around the given counter value.
     *
     * @param version the counter value, or {@code null} if absent
     */
    SyncdVersion(Long version) {
        this.version = version;
    }

    /**
     * Returns the version counter value.
     *
     * @return the version, or empty if absent
     */
    public OptionalLong version() {
        return version == null ? OptionalLong.empty() : OptionalLong.of(version);
    }

    /**
     * Sets the version counter value.
     *
     * @param version the counter value
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
