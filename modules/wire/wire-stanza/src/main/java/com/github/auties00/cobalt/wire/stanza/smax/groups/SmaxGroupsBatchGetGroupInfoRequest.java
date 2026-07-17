package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound {@code <iq type="get" xmlns="w:g2" to="g.us">} stanza that fetches metadata for several groups in one
 * round-trip.
 *
 * This request backs the bulk-fetch path used to hydrate community and sub-group panels and search results. The
 * relay accepts up to 10000 entries per request; callers should pre-batch larger working sets.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBatchGetGroupInfoRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetServerMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQGetRequestMixin")
public final class SmaxGroupsBatchGetGroupInfoRequest implements SmaxStanza.Request {
    /**
     * Holds the group {@link Jid}s to fetch metadata for.
     */
    private final List<Jid> groupJids;

    /**
     * Holds the optional query-context tag surfaced as the {@code <query context="...">} attribute.
     */
    private final SmaxGroupsBatchGetGroupInfoContext queryContext;

    /**
     * Constructs a request without a correlation token.
     *
     * @param groupJids the group {@link Jid}s to fetch
     * @throws NullPointerException     if {@code groupJids} is {@code null}
     * @throws IllegalArgumentException if {@code groupJids} is empty
     */
    public SmaxGroupsBatchGetGroupInfoRequest(List<Jid> groupJids) {
        this(groupJids, null);
    }

    /**
     * Constructs a request with the given query-context tag.
     *
     * The tag names the client maintenance path that issued the bulk fetch; the relay logs it but does not act on
     * it, so the returned metadata is identical regardless of the value. The supplied list is defensively copied.
     *
     * @param groupJids    the group {@link Jid}s to fetch
     * @param queryContext the optional query-context tag; may be {@code null}
     * @throws NullPointerException     if {@code groupJids} is {@code null}
     * @throws IllegalArgumentException if {@code groupJids} is empty
     */
    public SmaxGroupsBatchGetGroupInfoRequest(List<Jid> groupJids, SmaxGroupsBatchGetGroupInfoContext queryContext) {
        Objects.requireNonNull(groupJids, "groupJids cannot be null");
        if (groupJids.isEmpty()) {
            throw new IllegalArgumentException("groupJids cannot be empty");
        }
        this.groupJids = List.copyOf(groupJids);
        this.queryContext = queryContext;
    }

    /**
     * Returns the list of group {@link Jid}s carried by this request.
     *
     * @return an unmodifiable list of group {@link Jid}s; never {@code null} and never empty
     */
    public List<Jid> groupJids() {
        return groupJids;
    }

    /**
     * Returns the optional query-context tag.
     *
     * @return an {@link Optional} carrying the query-context tag, or empty when the caller did not supply one
     */
    public Optional<SmaxGroupsBatchGetGroupInfoContext> queryContext() {
        return Optional.ofNullable(queryContext);
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="g.us" type="get">
     *         <query context="<queryContext>">
     *             <group jid="<jid0>"/>
     *             <group jid="<jid1>"/>
     *             ...
     *         </query>
     *     </iq>
     * }
     * The {@code context} attribute is only emitted when {@link #queryContext()} is present.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the per-group {@code <query/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsBatchGetGroupInfoRequest",
            exports = "makeBatchGetGroupInfoRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var groupNodes = new ArrayList<Stanza>(groupJids.size());
        for (var groupJid : groupJids) {
            var groupNode = new StanzaBuilder()
                    .description("group")
                    .attribute("jid", groupJid)
                    .build();
            groupNodes.add(groupNode);
        }
        var queryBuilder = new StanzaBuilder()
                .description("query")
                .content(groupNodes);
        if (queryContext != null) {
            queryBuilder.attribute("context", queryContext.wireValue());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "get")
                .content(queryBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across both fields.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsBatchGetGroupInfoRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsBatchGetGroupInfoRequest) obj;
        return Objects.equals(this.groupJids, that.groupJids)
                && Objects.equals(this.queryContext, that.queryContext);
    }

    /**
     * Returns a hash composed of both fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJids, queryContext);
    }

    /**
     * Returns a debug string carrying both fields.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsBatchGetGroupInfoRequest[groupJids=" + groupJids
                + ", queryContext=" + queryContext + ']';
    }
}
