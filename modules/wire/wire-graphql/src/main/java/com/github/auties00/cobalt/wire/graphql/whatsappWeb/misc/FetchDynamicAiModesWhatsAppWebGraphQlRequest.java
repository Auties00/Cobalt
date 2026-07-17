package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the relay query that fetches the dynamic Meta AI mode catalogue.
 *
 * <p>The operation takes no variables; it asks the relay for the list of selectable Meta AI modes
 * (the experiences offered in the AI mode selector), returned under the plural linked root
 * {@code xfb_meta_ai_modes}. The reply is consumed through {@link FetchDynamicAiModesWhatsAppWebGraphQlResponse}.
 *
 * @see FetchDynamicAiModesWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchDynamicAIModesQuery")
public final class FetchDynamicAiModesWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchDynamicAIModesQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25335662402775799";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchDynamicAIModesQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchDynamicAIModesQuery";

    /**
     * Constructs a fetch-dynamic-AI-modes request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public FetchDynamicAiModesWhatsAppWebGraphQlRequest() {
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
