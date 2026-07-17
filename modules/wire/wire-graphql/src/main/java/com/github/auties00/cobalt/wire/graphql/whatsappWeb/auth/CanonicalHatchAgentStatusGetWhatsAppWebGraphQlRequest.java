package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the relay query that fetches the activity status of the caller's Hatch generative-AI agent
 * channel.
 *
 * <p>The operation takes no variables; it asks the relay for the current agent activity descriptor
 * tied to the authenticated WhatsApp Web session, returned under
 * {@code wa_genai_hatch_channel_metadata.agent_status}. The reply is consumed through
 * {@link CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse}.
 *
 * @see CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchAgentStatusGetQuery")
public final class CanonicalHatchAgentStatusGetWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchAgentStatusGetQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25845071235188952";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchAgentStatusGetQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCanonicalHatchAgentStatusGetQuery";

    /**
     * Constructs a Hatch agent-status query request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public CanonicalHatchAgentStatusGetWhatsAppWebGraphQlRequest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
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
     * @implNote This implementation returns the empty object {@code "{}"}: the operation declares no
     * GraphQL variables, so there is nothing to serialize.
     */
    @Override
    public String variables() {
        return "{}";
    }
}
