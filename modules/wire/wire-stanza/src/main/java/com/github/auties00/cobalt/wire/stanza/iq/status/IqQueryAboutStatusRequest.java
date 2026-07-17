package com.github.auties00.cobalt.wire.stanza.iq.status;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.Objects;

/**
 * Models the outbound legacy-IQ stanza that fetches a single user's about text over the
 * {@code status} namespace.
 *
 * <p>Dispatching this request as an {@link IqStanza.Request} produces an
 * {@code <iq xmlns="status" type="get">} envelope addressed to {@link JidServer#user()} and wrapping a
 * {@code <status>} child that holds a single {@code <user jid="...">} grandchild naming the queried
 * user. The relay replies with the about text nested under {@code <status><user><status>}, parsed by
 * {@link IqQueryAboutStatusResponse}.
 *
 * <p>This is a legacy fallback path; the modern about lookup goes through a MEX transport instead. The
 * type carries no {@code com.github.auties00.cobalt.meta.*} provenance annotation because the current
 * WhatsApp Web bundle no longer exposes this legacy {@code status}-namespace lookup through a
 * resolvable module, so no accurate mapping can be declared.
 */
public final class IqQueryAboutStatusRequest implements IqStanza.Request {
    /**
     * Holds the JID of the user whose about text is queried, stamped into the {@code jid} attribute of
     * the {@code <user>} grandchild.
     *
     * <p>Never {@code null}.
     */
    private final Jid jid;

    /**
     * Constructs a query-about-status request bound to the given user JID.
     *
     * @param jid the user JID; never {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    public IqQueryAboutStatusRequest(Jid jid) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
    }

    /**
     * Returns the user JID bound to this request.
     *
     * @return the JID; never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="status" type="get">} envelope addressed to
     * {@link JidServer#user()} wrapping a {@code <status>} child that holds a single
     * {@code <user jid="...">} grandchild naming the bound {@link #jid()}. The IQ {@code id} attribute
     * is assigned by the dispatch layer.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <status>}
     *         payload
     */
    @Override
    public StanzaBuilder toStanza() {
        var userNode = new StanzaBuilder()
                .description("user")
                .attribute("jid", jid)
                .build();
        var statusQuery = new StanzaBuilder()
                .description("status")
                .content(userNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "status")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(statusQuery);
    }

    /**
     * Compares this request with another object for value equality on the bound JID.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqQueryAboutStatusRequest} carrying an equal
     *         JID, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryAboutStatusRequest) obj;
        return Objects.equals(this.jid, that.jid);
    }

    /**
     * Returns a hash code derived from the bound JID.
     *
     * @return the field-derived hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid);
    }

    /**
     * Returns a debug string rendering the bound JID.
     *
     * @return a string representation of this request
     */
    @Override
    public String toString() {
        return "IqQueryAboutStatusRequest[jid=" + jid + ']';
    }
}
