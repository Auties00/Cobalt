package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that fetches the Facebook and WhatsApp ad-identity accounts
 * linked to the authenticated WhatsApp Business account.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the current
 * linked-accounts view tied to the linked account's Facebook access token, returned under
 * {@code xfb_wa_biz_linked_accounts}. The reply is consumed through
 * {@link LinkedAccountsGqlFacebookGraphQlResponse}.
 *
 * @see LinkedAccountsGqlFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebLinkedAccountsGQLQuery")
public final class LinkedAccountsGqlFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkedAccountsGQLQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25711291071821777";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkedAccountsGQLQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebLinkedAccountsGQLQuery";

    /**
     * Constructs a linked-accounts query request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public LinkedAccountsGqlFacebookGraphQlRequest() {
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
