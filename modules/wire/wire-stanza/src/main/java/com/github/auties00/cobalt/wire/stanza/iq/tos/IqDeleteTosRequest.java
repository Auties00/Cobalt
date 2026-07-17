package com.github.auties00.cobalt.wire.stanza.iq.tos;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="tos" type="set">} stanza that clears the server-side
 * accepted state for a single notice id.
 *
 * <p>Clearing the accepted state causes WhatsApp to re-prompt for that notice on the next surface
 * that gates on it. The bound notice id is one of the well-known terms-of-service or disclosure
 * identifiers (for example the 3P-disclosure id {@code "20210210"}, the bot agent, invoke and
 * shortcut terms ids, the newsletter producer, consumer and admin-invite terms ids, or the
 * Meta-messaging signal-sharing disclosure id). The reply is parsed by {@link IqDeleteTosResponse}.
 *
 * @implNote This implementation always dispatches. WhatsApp Web gates the dispatch on the
 *           {@code gkx 26258} server killswitch and no-ops when it is active; Cobalt does not
 *           consult the killswitch.
 */
@WhatsAppWebModule(moduleName = "WAWebTosJob")
public final class IqDeleteTosRequest implements IqStanza.Request {
    /**
     * Holds the notice id to clear, routed verbatim into the {@code <delete>} child's {@code id}
     * attribute.
     */
    private final String noticeId;

    /**
     * Constructs a delete-tos request bound to the given notice id.
     *
     * <p>The notice id is the literal identifier string (for example {@code "20210210"}). Unknown
     * ids are rejected upstream before the IQ is dispatched, so no validation happens here.
     *
     * @param noticeId the notice id to clear; never {@code null}
     * @throws NullPointerException if {@code noticeId} is {@code null}
     */
    public IqDeleteTosRequest(String noticeId) {
        this.noticeId = Objects.requireNonNull(noticeId, "noticeId cannot be null");
    }

    /**
     * Returns the bound notice id.
     *
     * @return the notice id; never {@code null}
     */
    public String noticeId() {
        return noticeId;
    }

    /**
     * Builds the outbound {@code <iq>} stanza wrapping the {@code <delete id="..."/>} payload.
     *
     * <p>The resulting {@link StanzaBuilder} is wire-ready except for the IQ {@code id} attribute,
     * which the dispatch layer assigns.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <delete>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "deleteTosState", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var deleteNode = new StanzaBuilder()
                .description("delete")
                .attribute("id", noticeId)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "tos")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(deleteNode);
    }

    /**
     * Compares this request to the given object for equality.
     *
     * <p>Two requests are equal when they bind the same notice id.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an {@link IqDeleteTosRequest} with an equal notice
     *         id, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqDeleteTosRequest) obj;
        return Objects.equals(this.noticeId, that.noticeId);
    }

    /**
     * Returns a hash code derived from the bound notice id.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(noticeId);
    }

    /**
     * Returns a debug string carrying the bound notice id.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqDeleteTosRequest[noticeId=" + noticeId + ']';
    }
}
