package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

/**
 * Models the outbound Waffle state-existence probe.
 * <p>
 * This request asks the relay whether the device is currently linked to a Facebook account; the reply
 * reports an unlinked, active, or paused state. The body carries only a {@code <timestamp/>} child, since the
 * relay identifies the caller from the authenticated session. The reply is parsed by
 * {@link SmaxWaffleStateExistsResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleStateExistsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleBaseIQGetRequestMixin")
public final class SmaxWaffleStateExistsRequest implements SmaxStanza.Request {
    /**
     * Holds the client wall-clock value stamped at request time, in seconds since the Unix epoch.
     */
    private final long timestamp;

    /**
     * Constructs a state-existence probe stamped at the given timestamp.
     * <p>
     * The timestamp is opaque to the relay and is echoed back inside the reply for clock-skew diagnostics.
     *
     * @param timestamp the request timestamp
     */
    public SmaxWaffleStateExistsRequest(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the request timestamp.
     *
     * @return the timestamp as supplied at construction time
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * The result is an {@code <iq xmlns="waffle" smax_id="142" type="get" to="s.whatsapp.net">} envelope
     * carrying a single {@code <timestamp/>} child. The dispatch path stamps a fresh {@code id} attribute on
     * every outbound stanza so the reply parser can match it back to this request.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <timestamp/>} payload; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleStateExistsRequest",
            exports = "makeStateExistsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var timestampNode = new StanzaBuilder()
                .description("timestamp")
                .content(timestamp)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "waffle")
                .attribute("smax_id", 142)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(timestampNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleStateExistsRequest} with an equal timestamp.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when both timestamps match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleStateExistsRequest) obj;
        return this.timestamp == that.timestamp;
    }

    /**
     * Returns a hash code derived from the timestamp.
     *
     * @return the {@link Long#hashCode(long)} of the timestamp
     */
    @Override
    public int hashCode() {
        return Long.hashCode(timestamp);
    }

    /**
     * Returns a debug rendering of this request.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleStateExistsRequest[timestamp=" + timestamp + ']';
    }
}
