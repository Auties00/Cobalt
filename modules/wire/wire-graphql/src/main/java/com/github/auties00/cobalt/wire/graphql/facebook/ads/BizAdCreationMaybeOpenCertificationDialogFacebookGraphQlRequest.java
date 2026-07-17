package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that decides whether the WhatsApp Business ad-creation flow must open the
 * ads-integrity certification dialog.
 *
 * <p>The operation takes no variables; it asks the relay for the viewer's current ads-integrity
 * certification state so the flow can gate ad creation behind the self-certification dialog. The
 * relay returns the state under the linked {@code viewer} root's scalar
 * {@code ad_integrity_certification}; the reply is consumed through
 * {@link BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse}.
 *
 * @see BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationMaybeOpenCertificationDialogQuery")
public final class BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationMaybeOpenCertificationDialogQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25319796354371579";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationMaybeOpenCertificationDialogQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationMaybeOpenCertificationDialogQuery";

    /**
     * Constructs a maybe-open-certification-dialog query request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAdCreationMaybeOpenCertificationDialogFacebookGraphQlRequest() {
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
