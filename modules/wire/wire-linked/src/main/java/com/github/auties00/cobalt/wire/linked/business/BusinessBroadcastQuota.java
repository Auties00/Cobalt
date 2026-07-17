package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Remaining broadcast (paid marketing) message quota for a WhatsApp
 * Business account.
 *
 * <p>Before a merchant sends another marketing broadcast, the composer
 * surfaces how many billable messages the account is still allowed to send
 * in the current period. WhatsApp answers that lookup with a quota object
 * tagged by a server-defined type marker.
 *
 * <p>The compiled WhatsApp Web bundle of snapshot {@code 1040120866}
 * selects only the {@linkplain #typename() type marker} on this object;
 * the concrete remaining-message counters are not requested on the wire,
 * so the model carries only the discriminator. The presence of a non-empty
 * type marker is itself the signal that the lookup succeeded and that the
 * account is subject to quota tracking.
 */
@ProtobufMessage(name = "BusinessBroadcastQuota")
public final class BusinessBroadcastQuota {
    /**
     * Server-defined type marker tagging the quota response. The full
     * marker set is not recoverable from the WhatsApp client, so the raw
     * marker is exposed as a string. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String typename;

    /**
     * Constructs a new {@code BusinessBroadcastQuota}. The reference
     * argument may be {@code null} when the server omitted the field.
     *
     * @param typename the type marker, or {@code null}
     */
    BusinessBroadcastQuota(String typename) {
        this.typename = typename;
    }

    /**
     * Returns the server-defined type marker tagging the quota response.
     *
     * @return the type marker, or empty when the server omitted it
     */
    public Optional<String> typename() {
        return Optional.ofNullable(typename);
    }

    /**
     * Returns a hash code derived from this quota's type marker.
     *
     * @return the hash code of the type marker
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(typename);
    }

    /**
     * Returns whether this quota is equal to the given object.
     *
     * <p>Two quotas are considered equal when they carry the same type
     * marker.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a
     *         {@code BusinessBroadcastQuota} with the same type marker
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastQuota that && Objects.equals(this.typename, that.typename);
    }

    /**
     * Returns a debug string describing this quota.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        return "BusinessBroadcastQuota[typename=" + typename + "]";
    }
}
