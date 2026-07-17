package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the inbound projection of a {@code <chatstate>} stanza pushed by
 * the relay when a peer or group participant raises a typing or paused event.
 *
 * <p>A consumer derives the composing or paused indicator from
 * {@link #stateType()} and routes it to the 1:1 handler when
 * {@link #stateSource()} resolves to a
 * {@link SmaxServerNotificationStateSource.FromUser} or to the group handler
 * when it resolves to a {@link SmaxServerNotificationStateSource.FromGroup}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInChatstateServerNotificationRequest")
public final class SmaxServerNotificationResponse implements SmaxStanza.Response {
    /**
     * Holds the state-source disjunction identifying who raised the event.
     */
    private final SmaxServerNotificationStateSource stateSource;

    /**
     * Holds the state-type disjunction identifying which indicator was raised,
     * composing or paused.
     */
    private final SmaxServerNotificationStateType stateType;

    /**
     * Holds the optional dev-only {@code <test config="..."/>} attribute
     * carried by the internal-test mixin.
     *
     * <p>The mixin is only ever populated by internal WhatsApp builds for
     * regression testing; production stanzas omit the {@code <test/>} child
     * and this field stays {@code null}.
     */
    private final String testConfig;

    /**
     * Constructs an inbound projection.
     *
     * @param stateSource the source disjunction; never {@code null}
     * @param stateType   the state-type disjunction; never {@code null}
     * @param testConfig  the optional dev-only test config; may be {@code null}
     * @throws NullPointerException if {@code stateSource} or {@code stateType} is {@code null}
     */
    public SmaxServerNotificationResponse(SmaxServerNotificationStateSource stateSource, SmaxServerNotificationStateType stateType, String testConfig) {
        this.stateSource = Objects.requireNonNull(stateSource, "stateSource cannot be null");
        this.stateType = Objects.requireNonNull(stateType, "stateType cannot be null");
        this.testConfig = testConfig;
    }

    /**
     * Returns the state-source disjunction.
     *
     * @return the source; never {@code null}
     */
    public SmaxServerNotificationStateSource stateSource() {
        return stateSource;
    }

    /**
     * Returns the state-type disjunction.
     *
     * @return the type; never {@code null}
     */
    public SmaxServerNotificationStateType stateType() {
        return stateType;
    }

    /**
     * Returns the optional dev-only test config.
     *
     * @return an {@link Optional} carrying the value, or empty when the
     *         {@code <test/>} child was absent
     */
    public Optional<String> testConfig() {
        return Optional.ofNullable(testConfig);
    }

    /**
     * Parses a {@code <chatstate>} stanza into an inbound projection.
     *
     * <p>Parsing requires the {@code <chatstate>} tag, a parseable
     * {@link SmaxServerNotificationStateSource} (either a
     * {@link SmaxServerNotificationStateSource.FromUser} or a
     * {@link SmaxServerNotificationStateSource.FromGroup}), and a parseable
     * {@link SmaxServerNotificationStateType} (either a
     * {@link SmaxServerNotificationStateType.Composing} or a
     * {@link SmaxServerNotificationStateType.Paused}). The dev-only
     * {@code <test config="..."/>} child is best-effort, and its absence does
     * not fail the parse. A schema mismatch yields {@link Optional#empty()}.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} on schema mismatch
     * instead of throwing, so callers can route the stanza through the
     * configured error policy.
     *
     * @param stanza the inbound {@code <chatstate/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty on schema mismatch
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxChatstateServerNotificationRPC",
            exports = "receiveServerNotificationRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInChatstateServerNotificationRequest",
            exports = "parseServerNotificationRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInChatstateInternalTestMixin",
            exports = "parseInternalTestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxServerNotificationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("chatstate")) {
            return Optional.empty();
        }
        var stateSource = SmaxServerNotificationStateSource.of(stanza).orElse(null);
        if (stateSource == null) {
            return Optional.empty();
        }
        var stateType = SmaxServerNotificationStateType.of(stanza).orElse(null);
        if (stateType == null) {
            return Optional.empty();
        }
        var testConfig = stanza.getChild("test")
                .flatMap(testNode -> testNode.getAttributeAsString("config"))
                .orElse(null);
        return Optional.of(new SmaxServerNotificationResponse(stateSource, stateType, testConfig));
    }

    /**
     * Returns whether the given object is a {@link SmaxServerNotificationResponse}
     * with an equal source, state-type, and test config.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when all three fields match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxServerNotificationResponse) obj;
        return Objects.equals(this.stateSource, that.stateSource)
                && Objects.equals(this.stateType, that.stateType)
                && Objects.equals(this.testConfig, that.testConfig);
    }

    /**
     * Returns a hash code derived from the source, state-type, and test
     * config.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(stateSource, stateType, testConfig);
    }

    /**
     * Returns a debug-friendly textual representation of this projection.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxServerNotificationResponse[stateSource=" + stateSource
                + ", stateType=" + stateType
                + ", testConfig=" + testConfig + ']';
    }
}
