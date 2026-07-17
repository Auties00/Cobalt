package com.github.auties00.cobalt.wire.stanza.iq.tos;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="tos" type="set">} stanza that pushes a batch of
 * locally-accepted terms-of-service or disclosure notices to the relay.
 *
 * <p>The caller filters the locally-accepted notice ids against the set the relay has not yet seen
 * and pushes them in a single batch. The relay's role is to record acceptance, not to gate, so the
 * caller may pre-mark notices as accepted before the server roundtrip completes. The reply is
 * parsed by {@link IqUpdateTosResponse}.
 *
 * @implNote The outbound payload is a single {@code <request type="session_update">} child carrying
 *           one {@code <notice id="..."/>} grandchild per accepted id. WhatsApp Web also retries the
 *           500 arm via exponential backoff, which Cobalt callers apply at the dispatch layer rather
 *           than here.
 */
@WhatsAppWebModule(moduleName = "WAWebTosJob")
public final class IqUpdateTosRequest implements IqStanza.Request {
    /**
     * Holds the notice ids being acknowledged, routed verbatim into one {@code <notice/>} child per
     * entry.
     */
    private final List<String> noticeIds;

    /**
     * Constructs an update-tos request bound to the given notice ids.
     *
     * <p>An empty list produces a {@code <request type="session_update">} child with no
     * grandchildren; WhatsApp Web skips the dispatch entirely in that case rather than emitting an
     * empty batch.
     *
     * @param noticeIds the notice ids being acknowledged; never {@code null}, may be empty
     * @throws NullPointerException if {@code noticeIds} is {@code null}
     */
    public IqUpdateTosRequest(List<String> noticeIds) {
        Objects.requireNonNull(noticeIds, "noticeIds cannot be null");
        this.noticeIds = List.copyOf(noticeIds);
    }

    /**
     * Returns the unmodifiable list of bound notice ids.
     *
     * @return the notice ids; never {@code null}
     */
    public List<String> noticeIds() {
        return noticeIds;
    }

    /**
     * Builds the outbound {@code <iq>} stanza wrapping the {@code <request type="session_update">}
     * payload.
     *
     * <p>The resulting {@link StanzaBuilder} is wire-ready except for the IQ {@code id} attribute,
     * which the dispatch layer assigns.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <request>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "updateTosState", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var noticeNodes = new ArrayList<Stanza>(noticeIds.size());
        for (var id : noticeIds) {
            var noticeNode = new StanzaBuilder()
                    .description("notice")
                    .attribute("id", id)
                    .build();
            noticeNodes.add(noticeNode);
        }
        var requestNode = new StanzaBuilder()
                .description("request")
                .attribute("type", "session_update")
                .content(noticeNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "tos")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(requestNode);
    }

    /**
     * Compares this request to the given object for equality.
     *
     * <p>Two requests are equal when they bind the same notice ids in the same order.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an {@link IqUpdateTosRequest} with equal notice ids,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqUpdateTosRequest) obj;
        return Objects.equals(this.noticeIds, that.noticeIds);
    }

    /**
     * Returns a hash code derived from the bound notice ids.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(noticeIds);
    }

    /**
     * Returns a debug string carrying the bound notice ids.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqUpdateTosRequest[noticeIds=" + noticeIds + ']';
    }
}
