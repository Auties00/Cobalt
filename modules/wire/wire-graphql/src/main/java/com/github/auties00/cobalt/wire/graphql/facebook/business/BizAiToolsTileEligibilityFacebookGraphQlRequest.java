package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that checks whether the caller is eligible to onboard the Meta AI business
 * agent from the WhatsApp Business AI tools tile.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the current onboarding
 * eligibility tied to the linked account, returned under the linked
 * {@code xfb_meta_ai_biz_agent_wa_onboarding_eligibility} root as the scalar {@code eligible}. The
 * reply is consumed through {@link BizAiToolsTileEligibilityFacebookGraphQlResponse}.
 *
 * @see BizAiToolsTileEligibilityFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiToolsTileEligibilityQuery")
public final class BizAiToolsTileEligibilityFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiToolsTileEligibilityQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27140128522251730";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiToolsTileEligibilityQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiToolsTileEligibilityQuery";

    /**
     * Constructs a business-AI-tools tile eligibility request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAiToolsTileEligibilityFacebookGraphQlRequest() {
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
