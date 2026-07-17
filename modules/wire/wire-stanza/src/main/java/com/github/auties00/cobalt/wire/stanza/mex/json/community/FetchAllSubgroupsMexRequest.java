package com.github.auties00.cobalt.wire.stanza.mex.json.community;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.mex.json.MexGroupQueryContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Outbound MEX request that fetches every subgroup belonging to a WhatsApp
 * community.
 *
 * <p>This is the primary query issued when a community is loaded. The reply,
 * modelled by {@link FetchAllSubgroupsMexResponse}, carries the default
 * announcement subgroup (always a single group) and the full edge list of
 * regular subgroups, each tagged with its id, subject, property flags
 * (general-chat, membership-approval mode, hidden) and the pending
 * membership-approval-request counter scoped to the current user. The
 * {@code queryContext} variable is set to {@link MexGroupQueryContext#INTERACTIVE} when the user
 * opens the community panel.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchAllSubgroupsJob")
public final class FetchAllSubgroupsMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the all-subgroups document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text
     * is never sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllSubgroupsJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "9935467776504344";

    /**
     * GraphQL operation name carried by this query.
     *
     * @implNote The WA Web operation function is named
     * {@code mexFetchAllSubgroups}, but the GraphQL document name reported for
     * telemetry is {@code mexCommunityGetSubgroups}; this constant keeps the
     * document name.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllSubgroupsJob", exports = "mexFetchAllSubgroups",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexCommunityGetSubgroups";

    /**
     * Community parent group identifier, or {@code null} to omit it.
     */
    private final String groupId;

    /**
     * Query-context tag bound to the {@code query_context} GraphQL variable; always emitted.
     */
    private final MexGroupQueryContext queryContext;

    /**
     * Subgroup hint identifier, or {@code null} to omit it.
     */
    private final String subGroupHintId;

    /**
     * Constructs a new request with the three GraphQL variables.
     *
     * <p>The {@code queryContext} tag is set to {@link MexGroupQueryContext#INTERACTIVE} when the
     * user is browsing the community panel. The {@code subGroupHintId} lets the relay prioritise
     * edges around a known subgroup. The {@code groupId} and {@code subGroupHintId} variables may be
     * {@code null} to drop them from the wire payload; {@code queryContext} is required and always
     * emitted, matching WA Web.
     *
     * @param groupId        the community parent group id, may be {@code null}
     * @param queryContext   the query-context tag; never {@code null}
     * @param subGroupHintId the subgroup hint identifier, may be {@code null}
     * @throws NullPointerException if {@code queryContext} is {@code null}
     */
    public FetchAllSubgroupsMexRequest(String groupId, MexGroupQueryContext queryContext, String subGroupHintId) {
        this.groupId = groupId;
        this.queryContext = Objects.requireNonNull(queryContext, "queryContext cannot be null");
        this.subGroupHintId = subGroupHintId;
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
     * @implNote This implementation streams the GraphQL variables through
     * fastjson2's {@link JSONWriter}, always emitting {@code query_context} and emitting
     * {@code group_id} and {@code sub_group_hint_id} only when their constructor argument is
     * non-null, matching WA Web which always sends the query context and omits other undefined
     * GraphQL variables. The envelope is built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllSubgroupsJob", exports = "mexFetchAllSubgroups",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            if (groupId != null) {
                writer.writeName("group_id");
                writer.writeColon();
                writer.writeString(groupId);
            }
            writer.writeName("query_context");
            writer.writeColon();
            writer.writeString(queryContext.wireValue());
            if (subGroupHintId != null) {
                writer.writeName("sub_group_hint_id");
                writer.writeColon();
                writer.writeString(subGroupHintId);
            }
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
