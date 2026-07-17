package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Outbound MEX mutation that updates a single property on a group (member add mode, announcement-only
 * flag, addressing-mode override, and similar) by issuing the corresponding GraphQL operation.
 *
 * <p>The relay rejects the mutation with HTTP 405 when the group is not {@code ACTIVE}. WA Web reuses
 * this operation both to set group properties one mutation at a time and to force an
 * addressing-mode override for tooling purposes.
 *
 * @implNote This implementation accepts the property-update payload as an opaque pre-serialised JSON
 * string for the {@code update} GraphQL variable, leaving payload composition to the caller; WA Web
 * builds the same payload from a typed property-update object at the call site. Cobalt does not
 * duplicate that property-codec surface.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateGroupPropertyJob")
public final class UpdateGroupPropertyMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the {@code WAWebMexUpdateGroupPropertyJobMutation}
     * document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateGroupPropertyJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "9418211574894172";

    /**
     * GraphQL operation name reported alongside this mutation when it is dispatched.
     *
     * <p>Tags the query in latency and error metrics; kept on the request for embedders mirroring
     * WhatsApp's telemetry surface.
     *
     * @implNote This implementation uses the GraphQL document name {@code mexUpdateGroupProperty},
     * which differs from the WA Web exporter function name {@code mexUpdateGroupPropertyJob}.
     */
    public static final String OPERATION_NAME = "mexUpdateGroupProperty";

    /**
     * Target group id bound to the {@code group_id} GraphQL variable.
     */
    private final String groupId;

    /**
     * Pre-serialised property update payload bound to the {@code update} GraphQL variable.
     */
    private final String update;

    /**
     * Constructs a new request with the two GraphQL variables.
     *
     * <p>The {@code update} is the pre-serialised JSON encoding of the single-property mutation, for
     * example:
     * {@snippet :
     *     var update = "{\"addressing_mode_override\":{\"addressing_mode\":\"LID\"}}";
     *     new UpdateGroupPropertyMexRequest(groupId, update);
     * }
     * Both {@code group_id} and {@code update} are declared top-level variables that WA Web always
     * sends, so callers are expected to supply non-null values; an empty mutation is normally
     * rejected by the relay.
     *
     * @param groupId the target group id
     * @param update  the pre-serialised JSON property-update payload
     */
    public UpdateGroupPropertyMexRequest(String groupId, String update) {
        this.groupId = groupId;
        this.update = update;
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
     * {@link JSONWriter}, always emitting the declared top-level {@code group_id} and {@code update}
     * variables, matching WA Web which always sends every declared variable. The {@code update} value
     * is written as a JSON-string literal with its content forwarded verbatim rather than re-parsed,
     * mirroring the WA Web call shape where the variable is already a JSON-serialisable scalar.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateGroupPropertyJob", exports = "mexUpdateGroupPropertyJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("group_id");
            writer.writeColon();
            writer.writeString(groupId);
            writer.writeName("update");
            writer.writeColon();
            writer.writeString(update);
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
