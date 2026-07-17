package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Automation settings of a Click-to-WhatsApp ad targeting spec.
 *
 * <p>An ad's targeting spec can delegate audience expansion to the platform. This model carries the
 * recovered automation flag, whether {@link #advantageAudience() Advantage audience} expansion is
 * enabled.
 */
@ProtobufMessage(name = "TargetingAutomation")
public final class TargetingAutomation {
    /**
     * Advantage-audience flag ({@code 1} enabled, {@code 0} disabled), or unset when the server
     * omitted it. This is the only automation flag the client sets.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer advantageAudience;

    /**
     * Constructs a new {@code TargetingAutomation}. The {@code advantageAudience} may be {@code null}
     * to leave it unset.
     *
     * @param advantageAudience the advantage-audience flag, or {@code null}
     */
    TargetingAutomation(Integer advantageAudience) {
        this.advantageAudience = advantageAudience;
    }

    /**
     * Returns the advantage-audience flag.
     *
     * @return an {@link OptionalInt} carrying the flag, or empty when unset
     */
    public OptionalInt advantageAudience() {
        return advantageAudience == null ? OptionalInt.empty() : OptionalInt.of(advantageAudience);
    }
}
