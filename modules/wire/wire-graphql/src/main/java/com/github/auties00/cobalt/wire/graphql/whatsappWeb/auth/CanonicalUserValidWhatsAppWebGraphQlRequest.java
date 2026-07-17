package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the relay query that validates whether the authenticated session is a canonical WhatsApp
 * user.
 *
 * <p>The operation takes no variables; it asks the relay to confirm that the session cookie resolves
 * to a valid canonical user. The relay returns the verdict under the linked
 * {@code xwa_canonical_user_valid} root; the reply is consumed through
 * {@link CanonicalUserValidWhatsAppWebGraphQlResponse}.
 *
 * @see CanonicalUserValidWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalUserValidQuery")
public final class CanonicalUserValidWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalUserValidQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25995999653397511";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalUserValidQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebCanonicalUserValidQuery";

    /**
     * Constructs a canonical-user-valid request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public CanonicalUserValidWhatsAppWebGraphQlRequest() {
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
