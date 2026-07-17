package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that lists the lead-generation flows of a WhatsApp Business AI agent.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the caller's lead-gen flows tied to the
 * linked account's Facebook access token, returned under the plural root
 * {@code xfb_maiba_gen_lead_gen_flow}. Each flow carries its configured fields and the captured lead
 * data, including each lead's consumer phone-number and LID addresses. The reply is consumed through
 * {@link BizAiLeadGenFormsFacebookGraphQlResponse}.
 *
 * @see BizAiLeadGenFormsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenFormsQuery")
public final class BizAiLeadGenFormsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenFormsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26699844229652541";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenFormsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiLeadGenFormsQuery";

    /**
     * Constructs a list-lead-gen-flows query request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAiLeadGenFormsFacebookGraphQlRequest() {
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
