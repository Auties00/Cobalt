package com.github.auties00.cobalt.wire.stanza.smax.presence;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <presence type? name?/>} availability broadcast.
 *
 * <p>This is the fire-and-forget stanza a client dispatches to announce its own presence to peers
 * that subscribed through {@link SmaxSubscribeRequest}. Both attributes are optional, so a single
 * dispatch can carry a pure available/unavailable transition, a pure push-name republish, or both
 * at once. The stanza is built by {@link #toStanza()} and serialised through the
 * {@link SmaxStanza.Request} dispatch contract.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPresenceAvailabilityRequest")
public final class SmaxAvailabilityRequest implements SmaxStanza.Request {
    /**
     * Holds the optional presence type rendered into the {@code type} attribute.
     *
     * <p>Typically {@code "available"} or {@code "unavailable"}, though the relay accepts any
     * string. When {@code null} the stanza degenerates to a pure name republish and the attribute
     * is dropped at render time.
     */
    private final String presenceType;

    /**
     * Holds the optional push-name rendered into the {@code name} attribute.
     *
     * <p>When {@code null} the attribute is dropped at render time and the relay reuses the
     * previously broadcast value.
     */
    private final String presenceName;

    /**
     * Constructs a new availability broadcast.
     *
     * <p>Both arguments are optional so callers may craft a pure type transition, a pure name
     * republish, or a combined stanza in a single dispatch.
     *
     * @param presenceType the optional presence type; may be {@code null}
     * @param presenceName the optional push-name; may be {@code null}
     */
    public SmaxAvailabilityRequest(String presenceType, String presenceName) {
        this.presenceType = presenceType;
        this.presenceName = presenceName;
    }

    /**
     * Returns the optional presence type.
     *
     * <p>Empty when this broadcast does not change the user's available/unavailable status.
     *
     * @return an {@link Optional} carrying the type
     */
    public Optional<String> presenceType() {
        return Optional.ofNullable(presenceType);
    }

    /**
     * Returns the optional push-name.
     *
     * <p>Empty when this broadcast does not republish the user's display name.
     *
     * @return an {@link Optional} carrying the name
     */
    public Optional<String> presenceName() {
        return Optional.ofNullable(presenceName);
    }

    /**
     * Builds the outbound {@code <presence type? name?/>} stanza ready for dispatch.
     *
     * <p>The {@link StanzaBuilder} is returned unbuilt so the dispatch path can stamp a fresh stanza
     * id before flushing. Null-valued attributes are dropped at render time by
     * {@link StanzaBuilder#attribute(String, String)}.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <presence type? name?/>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPresenceAvailabilityRequest",
            exports = "makeAvailabilityRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("presence")
                .attribute("type", presenceType)
                .attribute("name", presenceName);
    }

    /**
     * Compares this broadcast with another for value equality.
     *
     * <p>Two instances are equal when both the presence type and the push-name are equal.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} if {@code obj} is an equal {@link SmaxAvailabilityRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxAvailabilityRequest) obj;
        return Objects.equals(this.presenceType, that.presenceType)
                && Objects.equals(this.presenceName, that.presenceName);
    }

    /**
     * Returns a hash code derived from the presence type and push-name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(presenceType, presenceName);
    }

    /**
     * Returns a debug string exposing the presence type and push-name.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxAvailabilityRequest[presenceType=" + presenceType
                + ", presenceName=" + presenceName + ']';
    }
}
