package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the comet mutation that sends a WhatsApp Business payment-hub notification from the ad
 * management payment modal.
 *
 * <p>The mutation takes no variables; it asks the relay to dispatch the payment-hub notification tied
 * to the authenticated WhatsApp Business account when the ad management payment modal is shown. The
 * relay returns the dispatch outcome under the scalar
 * {@code xfb_wa_biz_send_payment_hub_notification}; the reply is consumed through
 * {@link BizAdManagementAdPaymentModalFacebookGraphQlResponse}.
 *
 * @see BizAdManagementAdPaymentModalFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdManagementAdPaymentModalMutation")
public final class BizAdManagementAdPaymentModalFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdManagementAdPaymentModalMutation.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "33275038202087396";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdManagementAdPaymentModalMutation.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdManagementAdPaymentModalMutation";

    /**
     * Constructs an ad-management payment-modal mutation request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAdManagementAdPaymentModalFacebookGraphQlRequest() {
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
