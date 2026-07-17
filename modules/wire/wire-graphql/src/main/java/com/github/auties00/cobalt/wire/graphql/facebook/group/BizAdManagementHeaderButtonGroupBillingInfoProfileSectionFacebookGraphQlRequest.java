package com.github.auties00.cobalt.wire.graphql.facebook.group;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that fetches the linked Facebook profile shown in the billing-info button
 * group of the WhatsApp Business ad-management header.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the {@code me} actor of
 * the linked account, returning the actor's {@code __typename} discriminator, its
 * {@code profile_picture} downloadable URI, and its {@code id}. The reply is consumed through
 * {@link BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse}.
 *
 * @see BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdManagementHeaderButtonGroupBillingInfoProfileSectionQuery")
public final class BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdManagementHeaderButtonGroupBillingInfoProfileSectionQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26201583379511786";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdManagementHeaderButtonGroupBillingInfoProfileSectionQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdManagementHeaderButtonGroupBillingInfoProfileSectionQuery";

    /**
     * Constructs a billing-info profile-section request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlRequest() {
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
