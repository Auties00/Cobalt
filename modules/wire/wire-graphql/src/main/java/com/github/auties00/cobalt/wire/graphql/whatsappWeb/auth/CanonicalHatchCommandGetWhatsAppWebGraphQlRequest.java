package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the relay query that fetches the slash-command catalog of the caller's Hatch generative-AI
 * agent.
 *
 * <p>The operation takes no variables; it asks the relay for the list of commands the Hatch agent
 * advertises for the authenticated WhatsApp Web session, returned under the plural root
 * {@code wa_genai_hatch_command_get}. The reply is consumed through
 * {@link CanonicalHatchCommandGetWhatsAppWebGraphQlResponse}.
 *
 * @see CanonicalHatchCommandGetWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchCommandGetQuery")
public final class CanonicalHatchCommandGetWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchCommandGetQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26009612435376221";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchCommandGetQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCanonicalHatchCommandGetQuery";

    /**
     * Constructs a Hatch command-catalog query request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public CanonicalHatchCommandGetWhatsAppWebGraphQlRequest() {
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
