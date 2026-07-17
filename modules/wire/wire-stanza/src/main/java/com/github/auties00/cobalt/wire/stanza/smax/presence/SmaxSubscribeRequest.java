package com.github.auties00.cobalt.wire.stanza.smax.presence;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <presence type="subscribe" to= name? context?/>} subscription stanza.
 *
 * <p>A client dispatches one of these per peer it wants presence updates from, for example when the
 * user opens a one-to-one chat or enters a group. The relay then starts pushing
 * {@link SmaxServerUpdateResponse} updates for the targeted peer until the subscription expires;
 * because the relay expires subscriptions, callers must re-issue them periodically to keep updates
 * flowing. The stanza is built by {@link #toStanza()} and serialised through the
 * {@link SmaxStanza.Request} dispatch contract.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPresenceSubscribeRequest")
public final class SmaxSubscribeRequest implements SmaxStanza.Request {
    /**
     * Holds the peer being subscribed to, rendered into the {@code to} attribute.
     *
     * <p>Either a user {@link Jid} (LID after one-to-one migration, PN before) or a group
     * {@link Jid}.
     */
    private final Jid presenceTo;

    /**
     * Holds the optional display-name hint rendered into the {@code name} attribute.
     *
     * <p>Used by the relay to surface the subscriber's push name in the peer's typing or online UI
     * when applicable; dropped at render time when {@code null}.
     */
    private final String presenceName;

    /**
     * Holds the optional parent group {@link Jid} rendered into the {@code context} attribute.
     *
     * <p>Supplied when the subscription targets a participant of an open group chat so the relay
     * scopes the push to that group's frame only; dropped at render time when {@code null}.
     */
    private final Jid presenceContext;

    /**
     * Constructs a new subscription request.
     *
     * @param presenceTo      the peer to subscribe to; never {@code null}
     * @param presenceName    the optional display-name hint; may be {@code null}
     * @param presenceContext the optional parent group {@link Jid}; may be {@code null}
     * @throws NullPointerException if {@code presenceTo} is {@code null}
     */
    public SmaxSubscribeRequest(Jid presenceTo, String presenceName, Jid presenceContext) {
        this.presenceTo = Objects.requireNonNull(presenceTo, "presenceTo cannot be null");
        this.presenceName = presenceName;
        this.presenceContext = presenceContext;
    }

    /**
     * Returns the peer {@link Jid} being subscribed to.
     *
     * @return the peer JID; never {@code null}
     */
    public Jid presenceTo() {
        return presenceTo;
    }

    /**
     * Returns the optional display-name hint.
     *
     * <p>Empty when no hint should be advertised.
     *
     * @return an {@link Optional} carrying the hint
     */
    public Optional<String> presenceName() {
        return Optional.ofNullable(presenceName);
    }

    /**
     * Returns the optional parent group {@link Jid}.
     *
     * <p>Empty when the subscription is not group-scoped.
     *
     * @return an {@link Optional} carrying the group JID
     */
    public Optional<Jid> presenceContext() {
        return Optional.ofNullable(presenceContext);
    }

    /**
     * Builds the outbound {@code <presence type="subscribe" to= name? context?/>} stanza ready for
     * dispatch.
     *
     * <p>The {@link StanzaBuilder} is returned unbuilt so the dispatch path can stamp a fresh stanza
     * id before flushing. Null-valued attributes are dropped at render time.
     *
     * @return a {@link StanzaBuilder} carrying the
     *         {@code <presence type="subscribe" to= name? context?/>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPresenceSubscribeRequest",
            exports = "makeSubscribeRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("presence")
                .attribute("type", "subscribe")
                .attribute("to", presenceTo)
                .attribute("name", presenceName)
                .attribute("context", presenceContext);
    }

    /**
     * Compares this request with another for value equality.
     *
     * <p>Two instances are equal when the peer, display-name hint, and parent group context are all
     * equal.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} if {@code obj} is an equal {@link SmaxSubscribeRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxSubscribeRequest) obj;
        return Objects.equals(this.presenceTo, that.presenceTo)
                && Objects.equals(this.presenceName, that.presenceName)
                && Objects.equals(this.presenceContext, that.presenceContext);
    }

    /**
     * Returns a hash code derived from the peer, display-name hint, and parent group context.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(presenceTo, presenceName, presenceContext);
    }

    /**
     * Returns a debug string exposing the peer, display-name hint, and parent group context.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxSubscribeRequest[presenceTo=" + presenceTo
                + ", presenceName=" + presenceName
                + ", presenceContext=" + presenceContext + ']';
    }
}
