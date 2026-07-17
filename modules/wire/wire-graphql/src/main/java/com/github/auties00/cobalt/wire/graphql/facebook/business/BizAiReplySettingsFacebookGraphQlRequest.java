package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that fetches the business AI assistant's automatic-reply settings.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the current reply chat trigger and the
 * bot's enabled-time window for the authenticated business account, returned under
 * {@code xfb_meta_ai_biz_agent_wa_reply_chat_trigger}. The reply is consumed through
 * {@link BizAiReplySettingsFacebookGraphQlResponse}.
 *
 * @see BizAiReplySettingsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReplySettingsQuery")
public final class BizAiReplySettingsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplySettingsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35327675310179742";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplySettingsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiReplySettingsQuery";

    /**
     * Constructs a fetch-reply-settings request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAiReplySettingsFacebookGraphQlRequest() {
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
    @WhatsAppWebExport(moduleName = "WAWebBizAiReplySettingsQuery", exports = "fetchReplySettings",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        return "{}";
    }
}
