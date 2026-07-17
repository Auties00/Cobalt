package com.github.auties00.cobalt.wire.stanza.smax.usernotice;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.List;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="tos" type="get">} stanza that polls the relay for the
 * current acceptance stage of a specific set of disclosures.
 *
 * <p>Unlike {@link SmaxUserNoticeGetDisclosuresRequest}, which returns every outstanding
 * disclosure, this stanza narrows the query to a known id set: one {@link DisclosureStageQuery}
 * per disclosure id to poll, each rendered as a {@code <get_disclosure_stage_by_id>} child. It
 * serves callers that only care about whether one particular disclosure has progressed, such as
 * a background soft-opt-in sync hitting the relay with a single biz-broadcast disclosure id. The
 * reply is parsed by {@link SmaxUserNoticeGetDisclosureStageByIdsResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutUserNoticeGetDisclosureStageByIdsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutUserNoticeBaseIQGetRequestMixin")
public final class SmaxUserNoticeGetDisclosureStageByIdsRequest implements SmaxStanza.Request {
    /**
     * Holds the per-disclosure queries that are rendered as {@code <get_disclosure_stage_by_id>}
     * children.
     */
    private final List<DisclosureStageQuery> queries;

    /**
     * Constructs a request carrying one {@link DisclosureStageQuery} per disclosure id to poll.
     *
     * <p>The relay returns a parallel list of stage entries in the reply.
     *
     * @param queries the per-disclosure queries
     * @throws NullPointerException if {@code queries} is {@code null}
     */
    public SmaxUserNoticeGetDisclosureStageByIdsRequest(List<DisclosureStageQuery> queries) {
        Objects.requireNonNull(queries, "queries cannot be null");
        this.queries = List.copyOf(queries);
    }

    /**
     * Returns the per-disclosure queries.
     *
     * <p>The returned {@link List} is unmodifiable.
     *
     * @return an unmodifiable {@link List} of {@link DisclosureStageQuery}
     */
    public List<DisclosureStageQuery> queries() {
        return queries;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="tos" type="get" to="s.whatsapp.net">} envelope nesting one
     * {@code <get_disclosure_stage_by_id id t/>} child per {@link DisclosureStageQuery}.
     *
     * @implNote
     * This implementation addresses the envelope to {@link JidServer#user()} and hard-codes the
     * {@code tos} namespace and {@code get} type, matching the wire shape of the polled relay
     * endpoint.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutUserNoticeGetDisclosureStageByIdsRequest",
            exports = "makeGetDisclosureStageByIdsRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "tos")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        for (var query : queries) {
            var child = new StanzaBuilder()
                    .description("get_disclosure_stage_by_id")
                    .attribute("id", query.disclosureId())
                    .attribute("t", query.timestampSeconds())
                    .build();
            iqBuilder.content(child);
        }
        return iqBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two requests are equal when they carry equal query lists.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUserNoticeGetDisclosureStageByIdsRequest) obj;
        return Objects.equals(this.queries, that.queries);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The hash is derived from the query list.
     */
    @Override
    public int hashCode() {
        return Objects.hash(queries);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the type name and the query list in the record-like form shared across the
     * {@code Smax} stanza family.
     */
    @Override
    public String toString() {
        return "SmaxUserNoticeGetDisclosureStageByIdsRequest[queries=" + queries + ']';
    }

    /**
     * Pairs a disclosure id with a client-side timestamp for a single
     * {@code <get_disclosure_stage_by_id id t/>} child.
     *
     * <p>The disclosure id is the value of a date-coded disclosure identifier; the timestamp is
     * the client-side wall-clock time in seconds the relay uses to decide whether the cached stage
     * is still fresh.
     */
    public static final class DisclosureStageQuery {
        /**
         * Holds the disclosure id to query.
         */
        private final long disclosureId;

        /**
         * Holds the client-side timestamp, in seconds since the UNIX epoch, used by the relay as a
         * freshness input.
         */
        private final long timestampSeconds;

        /**
         * Constructs a per-disclosure query pairing an id with a freshness timestamp.
         *
         * <p>The timestamp is the current wall-clock time in seconds; the relay uses it as a
         * freshness input.
         *
         * @param disclosureId     the disclosure id to query
         * @param timestampSeconds the client-side timestamp in seconds since the UNIX epoch
         */
        public DisclosureStageQuery(long disclosureId, long timestampSeconds) {
            this.disclosureId = disclosureId;
            this.timestampSeconds = timestampSeconds;
        }

        /**
         * Returns the disclosure id.
         *
         * <p>The value populates the {@code id} attribute in
         * {@link SmaxUserNoticeGetDisclosureStageByIdsRequest#toStanza()}.
         *
         * @return the disclosure id
         */
        public long disclosureId() {
            return disclosureId;
        }

        /**
         * Returns the client-side timestamp.
         *
         * <p>The value populates the {@code t} attribute in
         * {@link SmaxUserNoticeGetDisclosureStageByIdsRequest#toStanza()}.
         *
         * @return the timestamp in seconds since the UNIX epoch
         */
        public long timestampSeconds() {
            return timestampSeconds;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two queries are equal when both the disclosure id and the timestamp match.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (DisclosureStageQuery) obj;
            return this.disclosureId == that.disclosureId
                    && this.timestampSeconds == that.timestampSeconds;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The hash is derived from the disclosure id and the timestamp.
         */
        @Override
        public int hashCode() {
            return Objects.hash(disclosureId, timestampSeconds);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Renders the disclosure id and timestamp in the record-like form shared across the
         * {@code Smax} stanza family.
         */
        @Override
        public String toString() {
            return "SmaxUserNoticeGetDisclosureStageByIdsRequest.DisclosureStageQuery[disclosureId="
                    + disclosureId + ", timestampSeconds=" + timestampSeconds + ']';
        }
    }
}
