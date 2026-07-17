package com.github.auties00.cobalt.wire.graphql.facebook.auth;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that fetches the authorized-agent feature policy for the current session.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the feature-control policy that applies
 * to the WhatsApp session acting as an authorized agent, returned under
 * {@code whatsapp_authorized_agent_feature_policy}. WhatsApp Web's
 * {@code WAWebAuthAgentFeaturePolicyQuery} caches the result and treats a missing policy object as a
 * not-an-authorized-agent signal. The reply is consumed through
 * {@link AuthAgentFeaturePolicyFacebookGraphQlResponse}.
 *
 * @see AuthAgentFeaturePolicyFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebAuthAgentFeaturePolicyQuery")
public final class AuthAgentFeaturePolicyFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebAuthAgentFeaturePolicyQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26467789126176720";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebAuthAgentFeaturePolicyQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebAuthAgentFeaturePolicyQuery";

    /**
     * Constructs an authorized-agent feature-policy request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public AuthAgentFeaturePolicyFacebookGraphQlRequest() {
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
