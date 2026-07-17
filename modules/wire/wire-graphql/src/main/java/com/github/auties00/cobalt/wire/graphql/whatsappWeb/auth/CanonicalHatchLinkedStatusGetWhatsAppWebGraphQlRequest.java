package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the relay query that fetches the caller's Hatch GenAI channel linked status.
 *
 * <p>The operation takes no variables; it asks the relay for the authenticated session's Hatch
 * channel metadata and the embedded linked-status record. WhatsApp Web drives it from
 * {@code WAWebCanonicalHatchLinkedStatusGetQuery.fetchHatchLinkedStatus} to refresh the
 * {@code WAWebHatchLinkedStatusManager} cache. The relay returns the linked status under
 * {@code wa_genai_hatch_channel_metadata.linked_status}; the reply is consumed through
 * {@link CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse}.
 *
 * @see CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchLinkedStatusGetQuery")
public final class CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchLinkedStatusGetQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35121461644169181";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalHatchLinkedStatusGetQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCanonicalHatchLinkedStatusGetQuery";

    /**
     * Constructs a fetch-Hatch-linked-status request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlRequest() {
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
