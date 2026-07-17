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
 * Models the outbound {@code <iq xmlns="tos" type="get">} stanza that fetches the per-user accepted
 * state for a batch of terms-of-service or disclosure notice ids.
 *
 * <p>The caller passes the notice ids it tracks locally (for example the 3P disclosure, bot agent,
 * invoke and shortcut terms, business-bot terms, Meta-messaging signal-sharing disclosure,
 * newsletter producer, consumer and admin-invite terms, or business-broadcast terms). The relay
 * returns the current accepted state for each id plus a refresh interval that drives the cadence of
 * the local state-pull loop. The reply is parsed by {@link IqQueryTosResponse}.
 *
 * @implNote The outbound payload is a single {@code <request>} child carrying one
 *           {@code <notice id="..."/>} grandchild per requested id.
 */
@WhatsAppWebModule(moduleName = "WAWebTosJob")
public final class IqQueryTosRequest implements IqStanza.Request {
    /**
     * Holds the notice ids being queried, routed verbatim into one {@code <notice/>} child per
     * entry.
     */
    private final List<String> noticeIds;

    /**
     * Constructs a query-tos request bound to the given notice ids.
     *
     * <p>An empty list produces a {@code <request>} child with no grandchildren; the relay then
     * returns an empty notice list inside a still-valid envelope, which the caller treats as a
     * no-op.
     *
     * @param noticeIds the notice ids to query; never {@code null}, may be empty
     * @throws NullPointerException if {@code noticeIds} is {@code null}
     */
    public IqQueryTosRequest(List<String> noticeIds) {
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
     * Builds the outbound {@code <iq>} stanza wrapping the {@code <request>} payload.
     *
     * <p>The resulting {@link StanzaBuilder} is wire-ready except for the IQ {@code id} attribute,
     * which the dispatch layer assigns.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <request>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "queryTosState", adaptation = WhatsAppAdaptation.DIRECT)
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
                .content(noticeNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "tos")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .content(requestNode);
    }

    /**
     * Compares this request to the given object for equality.
     *
     * <p>Two requests are equal when they bind the same notice ids in the same order.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an {@link IqQueryTosRequest} with equal notice ids,
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
        var that = (IqQueryTosRequest) obj;
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
        return "IqQueryTosRequest[noticeIds=" + noticeIds + ']';
    }
}
