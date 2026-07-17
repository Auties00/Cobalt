package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the graph.whatsapp.com GraphQL mutation that updates the caller's Waffle FX WAMO universal opt-out (UOOM) state.
 *
 * <p>The operation takes no variables; it instructs the graph.whatsapp.com endpoint to propagate the caller's current
 * Global Privacy Control opt-out into the linked Meta advertising (Waffle FX) surfaces and returns
 * the outcome as the boolean scalar {@code xfb_waffle_fx_wamo_update_uoom}. WhatsApp Web treats a
 * {@code true} outcome as the signal to mark the local GPC-completed user preference. The reply is
 * consumed through {@link WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse}.
 *
 * @see WaffleFxwamoUpdateUoomWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleFXWAMOUpdateUOOMMutation")
public final class WaffleFxwamoUpdateUoomWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleFXWAMOUpdateUOOMMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "10031635203620145";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleFXWAMOUpdateUOOMMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebWaffleFXWAMOUpdateUOOMMutation";

    /**
     * Constructs a Waffle FX WAMO update-UOOM mutation request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public WaffleFxwamoUpdateUoomWhatsAppGraphQlRequest() {
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
