package com.github.auties00.cobalt.wire.stanza.smax.offlinebatch;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Represents the outbound {@code <ib><offline_batch count/></ib>} stanza.
 *
 * <p>This request drives the offline-stanza pump: it is dispatched after every
 * backlog-processing tick to tell the relay how many additional offline stanzas
 * the client is ready to absorb in the next batch. The cast-shape RPC has no
 * reply variant; the relay simply resumes flushing offline messages until the
 * announced budget is consumed. Cobalt issues it from
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#acknowledgeOfflineBatch(int)}
 * to keep the pump flowing across the connection's post-login backlog drain.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutOfflineBatchRequest")
public final class SmaxOfflineBatchRequest implements SmaxStanza.Request {
    /**
     * Holds the number of offline stanzas the client is ready to absorb in the
     * upcoming batch.
     *
     * @implNote
     * This implementation stores the raw {@code int} value and does not
     * pre-validate its range; the count is sourced from the caller's own
     * offline-pump counter, which never produces a negative budget.
     */
    private final int offlineBatchCount;

    /**
     * Constructs a request announcing the given offline-batch budget.
     *
     * @param offlineBatchCount the expected offline-batch size
     */
    public SmaxOfflineBatchRequest(int offlineBatchCount) {
        this.offlineBatchCount = offlineBatchCount;
    }

    /**
     * Returns the announced offline-batch budget.
     *
     * @return the count
     */
    public int offlineBatchCount() {
        return offlineBatchCount;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces {@code <ib><offline_batch count="N"/></ib>}, where {@code N}
     * is {@link #offlineBatchCount()}. The {@code <ib>} envelope carries no
     * {@code id} attribute because the cast-shape RPC is fire-and-forget.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <ib>} envelope and the
     *         {@code <offline_batch/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutOfflineBatchRequest",
            exports = "makeBatchRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var offlineBatchNode = new StanzaBuilder()
                .description("offline_batch")
                .attribute("count", offlineBatchCount)
                .build();
        return new StanzaBuilder()
                .description("ib")
                .content(offlineBatchNode);
    }

    /**
     * Returns whether the given object is a {@link SmaxOfflineBatchRequest} with
     * an equal batch count.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when both batch counts match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxOfflineBatchRequest) obj;
        return this.offlineBatchCount == that.offlineBatchCount;
    }

    /**
     * Returns a hash code derived from the batch count.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(offlineBatchCount);
    }

    /**
     * Returns a debug-friendly textual representation of this request.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxOfflineBatchRequest[offlineBatchCount=" + offlineBatchCount + ']';
    }
}
