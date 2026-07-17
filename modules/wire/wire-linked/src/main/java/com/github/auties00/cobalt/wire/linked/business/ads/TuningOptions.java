package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Tuning options applied when re-running regulated-category targeting checks for a Click-to-WhatsApp
 * ad.
 *
 * <p>When the caller switches an ad's special ad category, the server can re-derive the compliant
 * targeting spec. This model carries the single tuning knob controlling whether custom audiences are
 * cleared as part of that re-derivation.
 */
@ProtobufMessage(name = "TuningOptions")
public final class TuningOptions {
    /**
     * Whether custom audiences are cleared when the targeting spec is re-derived.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean clearCustomAudiences;

    /**
     * Constructs a new {@code TuningOptions}.
     *
     * @param clearCustomAudiences whether to clear custom audiences on re-derivation
     */
    TuningOptions(boolean clearCustomAudiences) {
        this.clearCustomAudiences = clearCustomAudiences;
    }

    /**
     * Returns whether custom audiences are cleared when the targeting spec is re-derived.
     *
     * @return {@code true} when custom audiences are cleared, {@code false} otherwise
     */
    public boolean clearCustomAudiences() {
        return clearCustomAudiences;
    }
}
