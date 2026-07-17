package com.github.auties00.cobalt.wire.stanza.mex.json.community;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.mex.json.MexGroupQueryContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Outbound MEX request that fetches the current participant counts for the subgroups of a community.
 *
 * <p>This query backs the community-panel participant-count refresh path: it queries the community by
 * id and reads only the {@code id} and {@code total_participants_count} of each subgroup edge rather
 * than reloading full subgroup metadata, and is typically issued when the user scrolls or sorts the
 * community subgroup list. The {@code group_jid} names the parent community whose subgroups are read,
 * while {@code sub_group_jid_hint} names the subgroup of interest so the relay can prioritise it. The
 * reply is modelled by {@link QuerySubgroupParticipantCountMexResponse}.
 */
public final class QuerySubgroupParticipantCountMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the participant-count document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text
     * is never sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexQuerySubgroupParticipantCountJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "24079399904996141";

    /**
     * GraphQL operation name carried by this query.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexQuerySubgroupParticipantCountJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexQuerySubgroupParticipantCount";

    /**
     * Holds the Jid string of the parent community whose subgroups are queried.
     */
    private final String communityJid;

    /**
     * Holds the Jid string of the subgroup of interest, sent to the relay as the prioritisation hint.
     */
    private final String subgroupJid;

    /**
     * Constructs a request that reads the participant counts of the given community's subgroups.
     *
     * <p>The {@code communityJid} is written as the {@code group_jid} input field, the fixed
     * {@code query_context} of {@link MexGroupQueryContext#INTERACTIVE} tags the query as
     * foreground-driven, and {@code subgroupJid} is written as {@code sub_group_jid_hint} so the relay
     * prioritises the subgroup the caller cares about.
     *
     * @param communityJid the Jid of the parent community
     * @param subgroupJid  the Jid of the subgroup of interest
     */
    public QuerySubgroupParticipantCountMexRequest(String communityJid, String subgroupJid) {
        this.communityJid = communityJid;
        this.subgroupJid = subgroupJid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the
     * {@code {variables: {input: {group_jid, query_context, sub_group_jid_hint}}}} payload consumed by
     * the persisted-query identified by {@link #QUERY_ID}.
     *
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, pinning {@code query_context} to {@link MexGroupQueryContext#INTERACTIVE}
     * exactly as WhatsApp Web's subgroup job does, and builds the envelope through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}. Any {@link IOException} raised by
     * the in-memory writer is wrapped in an {@link UncheckedIOException}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexQuerySubgroupParticipantCountJob", exports = "mexQuerySubgroupParticipantCountJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("group_jid");
            writer.writeColon();
            writer.writeString(communityJid);
            writer.writeName("query_context");
            writer.writeColon();
            writer.writeString(MexGroupQueryContext.INTERACTIVE.wireValue());
            writer.writeName("sub_group_jid_hint");
            writer.writeColon();
            writer.writeString(subgroupJid);
            writer.endObject();
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
