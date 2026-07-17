package com.github.auties00.cobalt.wire.stanza.smax.unifiedsession;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Announces this device's WAM user-journey analytics session id to the relay.
 *
 * <p>WhatsApp Web derives the session id from a rolling 7-day clock
 * ({@code (now + 3d) mod 7d}) and tags WAM events emitted from the Channels
 * surface, the Forward-message flow, the CTWA ad-creation pipeline, and the
 * signup funnel with it. Cobalt does not run those user-journey loggers
 * itself; the request is exposed through
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#joinUnifiedSession(String)}
 * for embedders that mirror that telemetry surface. The operation is a
 * {@code cast}-shape RPC: it is one-way outbound and carries no reply, so it
 * implements only {@link SmaxStanza.Request}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutUnifiedSessionShareRequest")
public final class SmaxUnifiedSessionShareRequest implements SmaxStanza.Request {
    /**
     * Holds the opaque user-journey session id token announced to the relay.
     */
    private final String unifiedSessionId;

    /**
     * Constructs a request announcing the given session id.
     *
     * <p>The relay accepts any non-{@code null} string verbatim, and the
     * caller is responsible for re-issuing the request when its rolling-clock
     * derivation produces a new id.
     *
     * @param unifiedSessionId the session id token; never {@code null}
     * @throws NullPointerException if {@code unifiedSessionId} is {@code null}
     */
    public SmaxUnifiedSessionShareRequest(String unifiedSessionId) {
        this.unifiedSessionId = Objects.requireNonNull(unifiedSessionId, "unifiedSessionId cannot be null");
    }

    /**
     * Returns the announced session id.
     *
     * @return the id; never {@code null}
     */
    public String unifiedSessionId() {
        return unifiedSessionId;
    }

    /**
     * Builds the outbound stanza ready for dispatch.
     *
     * <p>Produces {@code <ib><unified_session id="..."/></ib>}. The envelope
     * carries no {@code id} attribute because, as a {@code cast}-shape RPC, no
     * reply is expected.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <ib>} envelope and the
     *         {@code <unified_session/>} payload; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutUnifiedSessionShareRequest",
            exports = "makeShareRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var unifiedSessionNode = new StanzaBuilder()
                .description("unified_session")
                .attribute("id", unifiedSessionId)
                .build();
        return new StanzaBuilder()
                .description("ib")
                .content(unifiedSessionNode);
    }

    /**
     * Returns whether the given object is a
     * {@link SmaxUnifiedSessionShareRequest} with an equal session id.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when both ids match, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUnifiedSessionShareRequest) obj;
        return Objects.equals(this.unifiedSessionId, that.unifiedSessionId);
    }

    /**
     * Returns a hash code derived from the session id.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(unifiedSessionId);
    }

    /**
     * Returns a debug-friendly textual representation of this request.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxUnifiedSessionShareRequest[unifiedSessionId=" + unifiedSessionId + ']';
    }
}
