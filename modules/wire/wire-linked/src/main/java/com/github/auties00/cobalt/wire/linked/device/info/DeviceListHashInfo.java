package com.github.auties00.cobalt.wire.linked.device.info;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the summary information needed to query a user's device list
 * through USync without re-downloading the full list.
 *
 * <p>When asking the server for a user's devices via the USync
 * {@code devices} protocol, the client can attach a hash of its current
 * view of the user together with the associated timestamps. The server
 * compares the hash and, when it matches, replies with a short "no
 * change" response instead of the full list. This is the mechanism that
 * powers cheap delta updates of device lists.
 *
 * <p>A {@code DeviceListHashInfo} groups the three values that participate
 * in such a query:
 * <ul>
 *   <li>a compact hash of the user's known devices;</li>
 *   <li>the timestamp associated with the hashed snapshot;</li>
 *   <li>optionally, the expected next timestamp communicated by the
 *   server, which can be echoed back to prove the client is in sync.</li>
 * </ul>
 *
 * <p>Instances are immutable.
 */
@ProtobufMessage
public final class DeviceListHashInfo {
    /**
     * The compact hash of the user's known devices.
     *
     * <p>Computed client side by folding the device ids and their
     * identity key indexes into a short string that the server can check
     * in constant time.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String hash;

    /**
     * The timestamp of the device list snapshot that produced
     * {@link #hash}.
     *
     * <p>Sent alongside the hash so that the server can reject the query
     * when the client's snapshot is too old, even if the hash happens to
     * still match.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant timestamp;

    /**
     * The optional expected timestamp previously communicated by the
     * server for the next version of the device list.
     *
     * <p>Echoing this value back lets the server confirm that the client
     * has processed the last expected timestamp hint. May be {@code null}
     * when no expectation has been received yet.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant expectedTimestamp;

    /**
     * Creates a new hash info with the given fields.
     *
     * <p>This constructor is package-private; use
     * {@code DeviceListHashInfoBuilder} to construct instances.
     *
     * @param hash              the hash of the user's known devices
     * @param timestamp         the timestamp associated with the hash
     * @param expectedTimestamp the expected next timestamp, or {@code null}
     */
    DeviceListHashInfo(String hash, Instant timestamp, Instant expectedTimestamp) {
        this.hash = hash;
        this.timestamp = timestamp;
        this.expectedTimestamp = expectedTimestamp;
    }

    /**
     * Returns the compact hash of the user's known devices.
     *
     * @return the device hash
     */
    public String hash() {
        return hash;
    }

    /**
     * Returns the timestamp of the device list snapshot that produced
     * the hash.
     *
     * @return the snapshot timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the expected next timestamp previously communicated by the
     * server, if any.
     *
     * @return an {@link Optional} holding the expected timestamp when
     *         available, otherwise {@link Optional#empty()}
     */
    public Optional<Instant> expectedTimestamp() {
        return Optional.ofNullable(expectedTimestamp);
    }

    /**
     * Compares this hash info to another object for structural equality.
     *
     * <p>Two instances are equal when they share the same hash, timestamp
     * and expected timestamp.
     *
     * @param o the object to compare with
     * @return {@code true} if {@code o} is a {@code DeviceListHashInfo}
     *         with the same fields, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DeviceListHashInfo that
                            && Objects.equals(hash, that.hash)
                            && Objects.equals(timestamp, that.timestamp)
                            && Objects.equals(expectedTimestamp, that.expectedTimestamp);
    }

    /**
     * Returns a hash code derived from every field of this info.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(hash, timestamp, expectedTimestamp);
    }

    /**
     * Returns a human readable representation of this hash info suitable
     * for logging.
     *
     * @return a string describing the hash, timestamp and expected
     *         timestamp
     */
    @Override
    public String toString() {
        return "DeviceListHashInfo[" +
               "hash=" + hash + ", " +
               "timestamp=" + timestamp + ", " +
               "expectedTimestamp=" + expectedTimestamp + ']';
    }
}
