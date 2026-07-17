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
 * Outbound MEX request that fetches the list of public subgroups suggested to
 * the user as candidates for a given community.
 *
 * <p>This query backs the suggested-subgroups picker in the community
 * management UI. Suggestions include existing groups the user already belongs
 * to that could be promoted into the community; the reply, modelled by
 * {@link FetchSubgroupSuggestionsMexResponse}, carries each candidate's id,
 * subject, description, creator, creation timestamp, participant count and the
 * hidden-from-directory flag. The {@code queryContext} variable is set to
 * {@link MexGroupQueryContext#INTERACTIVE} when the user opens the picker.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchSubgroupSuggestionsJob")
public final class FetchSubgroupSuggestionsMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the subgroup-suggestions document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text
     * is never sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchSubgroupSuggestionsJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "23972005349071865";

    /**
     * GraphQL operation name carried by this query.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchSubgroupSuggestionsJob", exports = "mexFetchSubgroupSuggestions",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexFetchSubgroupSuggestions";

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
     * user is browsing the picker. The {@code subGroupHintId} lets the relay prioritise suggestions
     * around a known subgroup. The {@code groupId} and {@code subGroupHintId} variables may be
     * {@code null} to drop them from the wire payload; {@code queryContext} is required and always
     * emitted, matching WA Web.
     *
     * @param groupId        the community parent group id, may be {@code null}
     * @param queryContext   the query-context tag; never {@code null}
     * @param subGroupHintId the subgroup hint identifier, may be {@code null}
     * @throws NullPointerException if {@code queryContext} is {@code null}
     */
    public FetchSubgroupSuggestionsMexRequest(String groupId, MexGroupQueryContext queryContext, String subGroupHintId) {
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
     * non-null. The envelope is built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchSubgroupSuggestionsJob", exports = "mexFetchSubgroupSuggestions",
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
