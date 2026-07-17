package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.mex.json.MexGroupQueryContext;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Outbound MEX query that fetches the full metadata snapshot for a group, community or community
 * subgroup; bot participants are excluded from the participant edge list.
 *
 * <p>The reply carries the four-way group profile (regular group, community, default subgroup,
 * subgroup) including creator identity, subject and description (with author and edit timestamps),
 * participant edges, ephemeral timer, membership-approval state, LID migration state,
 * parent-community link and group-lock status. The bot-inclusive variant is
 * {@link FetchGroupInfoIncludBotsMexRequest}, used when the open-group bot gating flag is on.
 *
 * @implNote This implementation always emits the {@code query_context} variable as a typed
 * {@link MexGroupQueryContext}, mirroring WA Web which never omits it; an absent surface tag maps to
 * {@link MexGroupQueryContext#UNKNOWN}. The normalisation from WA Web's internal surface tags to the
 * wire enum is carried by {@link MexGroupQueryContext} itself.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInfoJob")
public final class FetchGroupInfoMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the {@code WAWebMexFetchGroupInfoJobQuery} document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "26570027442651356";

    /**
     * GraphQL operation name reported alongside this query when it is dispatched.
     *
     * <p>Tags the query in latency and error metrics; kept on the request for embedders mirroring
     * WhatsApp's telemetry surface.
     */
    public static final String OPERATION_NAME = "mexGetGroupInfo";

    /**
     * Target group identifier bound to the {@code id} GraphQL variable.
     */
    private final String id;

    /**
     * Username-projection toggle bound to the {@code include_username} GraphQL variable; always
     * emitted, with a {@code null} value written as {@code false}.
     */
    private final Boolean includeUsername;

    /**
     * Participant-set hash bound to the {@code participants_phash} GraphQL variable, enabling the
     * relay to skip the edge list when the local cache matches.
     */
    private final String participantsPhash;

    /**
     * Query-context tag bound to the {@code query_context} GraphQL variable; always emitted.
     */
    private final MexGroupQueryContext queryContext;

    /**
     * Constructs a new request with the four GraphQL variables.
     *
     * <p>The {@code includeUsername} flag controls whether the relay projects participant usernames,
     * mirroring WA Web's username-display gating. The {@code participantsPhash} is the rolling hash
     * maintained on the cached participant set; sending it lets the relay reply with
     * {@code participants_phash_match = true} and skip the edge list to save bandwidth. The
     * {@code id} and {@code participantsPhash} arguments may each be {@code null} to omit their
     * variable; {@code includeUsername} is always emitted, with a {@code null} value written as
     * {@code false}; {@code queryContext} is required and always emitted, matching WA Web which never
     * sends this query without it.
     *
     * @param id                 the target group identifier, may be {@code null} to omit
     * @param includeUsername    whether to project usernames on participant edges; a {@code null} value is sent as {@code false}
     * @param participantsPhash  the rolling participant-set hash, may be {@code null} to omit
     * @param queryContext       the query-context tag; never {@code null}
     * @throws NullPointerException if {@code queryContext} is {@code null}
     */
    public FetchGroupInfoMexRequest(String id, Boolean includeUsername, String participantsPhash, MexGroupQueryContext queryContext) {
        this.id = id;
        this.includeUsername = includeUsername;
        this.participantsPhash = participantsPhash;
        this.queryContext = Objects.requireNonNull(queryContext, "queryContext cannot be null");
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
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, always emitting the declared top-level variables {@code include_username}
     * (a {@code null} value written as {@code false}) and {@code query_context}, and emitting the
     * optional {@code id} and {@code participants_phash} fields only when their corresponding
     * constructor argument is non-null, matching WA Web which always sends the declared boolean and
     * context variables and omits other undefined GraphQL variables. The wrapped envelope is built
     * through {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJob", exports = "mexGetGroupInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
            }
            writer.writeName("include_username");
            writer.writeColon();
            writer.writeBool(includeUsername != null && includeUsername);
            if (participantsPhash != null) {
                writer.writeName("participants_phash");
                writer.writeColon();
                writer.writeString(participantsPhash);
            }
            writer.writeName("query_context");
            writer.writeColon();
            writer.writeString(queryContext.wireValue());
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
