package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

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
 * Requests a Data Subject Access Request (DSAR) reference number for a given entity.
 *
 * <p>The mutation targets a DSAR-eligible entity (typically a newsletter) and the relay generates a
 * reference number the user can quote when checking on the request's processing state; that number is
 * surfaced through {@link GetDsbInfoMexResponse#referenceNumber()}. The lone {@code entity_id}
 * variable identifies the entity the data-subject request is filed against and may be omitted.
 *
 * @implNote This implementation models the operation as a MEX mutation, because the relay records side
 * effects when it generates the reference, even though the call site reads as a getter. The
 * {@code entity_id} variable is omitted from the wire envelope when {@code null}, whereas WhatsApp Web
 * always passes the user-supplied entity id straight through.
 */
@WhatsAppWebModule(moduleName = "WAWebMexGetDsbInfoJob")
public final class GetDsbInfoMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the {@code WAWebMexGetDsbInfoJobMutation}
     * document.
     *
     * <p>The relay maps this identifier to a server-side persisted mutation and never sees the GraphQL
     * text on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetDsbInfoJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "9982897848413251";

    /**
     * Holds the GraphQL operation name carried by this mutation.
     *
     * <p>WhatsApp Web's MEX perf tracker uses the name to tag the query in latency and error metrics;
     * Cobalt keeps it on the request for embedders mirroring that telemetry surface.
     */
    public static final String OPERATION_NAME = "mexGetDsbInfo";

    /**
     * Holds the entity identifier bound to the {@code variables.input.entity_id} GraphQL variable, or
     * {@code null} when the variable is omitted from the wire envelope.
     */
    private final String entityId;

    /**
     * Constructs a new request targeting the given entity.
     *
     * <p>The {@code entityId} is the newsletter, or other DSAR-eligible entity, identifier the
     * data-subject request is filed against; passing {@code null} omits the variable from the wire
     * envelope.
     *
     * @param entityId the entity identifier whose data-subject request should be initiated, may be
     *                 {@code null} to omit
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetDsbInfoJob", exports = "mexGetDsbInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public GetDsbInfoMexRequest(String entityId) {
        this.entityId = entityId;
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
     * {@link JSONWriter}, opens a nested {@code input} object, emits the {@code entity_id} field only
     * when {@link #entityId} is non-{@code null}, then wraps the payload via
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetDsbInfoJob", exports = "mexGetDsbInfo",
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
            if (entityId != null) {
                writer.writeName("entity_id");
                writer.writeColon();
                writer.writeString(entityId);
            }
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
