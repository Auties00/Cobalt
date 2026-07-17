package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the graph.whatsapp.com GraphQL mutation that fetches the caller's Waffle FX service data for the cross-posting
 * and FB/IG account-linking surfaces.
 *
 * <p>The operation takes no variables; it asks the graph.whatsapp.com endpoint for the linked-services snapshot returned under {@code waffle_fx_service_data}. The snapshot
 * carries the per-destination cross-posting account list, the additional-feature-set eligibility, and
 * the foreground-app-to-WhatsApp link eligibility flags. The reply is consumed through
 * {@link WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse}.
 *
 * @see WaffleFxServiceDataQueryV2WhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleFXServiceDataQueryV2Mutation")
public final class WaffleFxServiceDataQueryV2WhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleFXServiceDataQueryV2Mutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9475021792620702";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleFXServiceDataQueryV2Mutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebWaffleFXServiceDataQueryV2Mutation";

    /**
     * Constructs a Waffle FX service-data mutation request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public WaffleFxServiceDataQueryV2WhatsAppGraphQlRequest() {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
